package com.staticum.diariocalorico.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private sealed class TextFetchResult {
    data class Success(val text: String) : TextFetchResult()
    data class Error(val message: String) : TextFetchResult()
}

/** Un fallo puntual (de un modelo/intento) durante una llamada a Gemini, para diagnóstico posterior. */
data class GeminiAttemptLog(
    val timestamp: Instant,
    val context: String,
    val model: String?,
    val message: String
)

/**
 * Algunas operadoras/redes filtran o tienen resolución DNS poco confiable específicamente para
 * dominios de APIs de IA (generativelanguage.googleapis.com), aunque el resto de la conexión
 * funcione bien. Cuando el DNS del sistema falla, se reintenta vía DNS-over-HTTPS de Cloudflare
 * (consultado por IP fija, sin depender del DNS del operador) antes de darse por vencido.
 */
private class FallbackDns : Dns {
    private val dohClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    private val dohJson = Json { ignoreUnknownKeys = true }

    override fun lookup(hostname: String): List<InetAddress> {
        try {
            return Dns.SYSTEM.lookup(hostname)
        } catch (systemFailure: UnknownHostException) {
            val viaDoh = resolveViaDoh(hostname)
            if (viaDoh.isNotEmpty()) return viaDoh
            throw systemFailure
        }
    }

    private fun resolveViaDoh(hostname: String): List<InetAddress> {
        return try {
            val request = Request.Builder()
                .url("https://1.1.1.1/dns-query?name=$hostname&type=A")
                .addHeader("Accept", "application/dns-json")
                .build()
            dohClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string().orEmpty()
                val root = dohJson.parseToJsonElement(body) as? JsonObject ?: return emptyList()
                val answers = root["Answer"] as? JsonArray ?: return emptyList()
                answers.mapNotNull { answer ->
                    ((answer as? JsonObject)?.get("data") as? JsonPrimitive)?.content
                        ?.let { ip -> runCatching { InetAddress.getByName(ip) }.getOrNull() }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class GeminiClient(
    private val apiKey: String,
    private val onLog: suspend (GeminiAttemptLog) -> Unit = {}
) {

    // Timeouts acotados a propósito: antes (30s/60s) un intento fallido podía tardar hasta
    // 90s en reportarse, y con varios modelos de respaldo y reintentos la app podía quedar
    // "pensando" varios minutos sin ninguna señal de que algo falló. Con timeouts más cortos
    // el límite global (ver overallTimeoutMs en fetchTextWithFallback) se respeta de verdad.
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(18, TimeUnit.SECONDS)
        .dns(FallbackDns())
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Orden de modelos a intentar. Si uno responde con error transitorio (503/429, sobrecarga
     * o modelo no disponible) se reintenta con el siguiente antes de reportar fallo al usuario.
     *
     * "gemini-flash-latest" va primero a propósito: es un alias que Google mantiene apuntando
     * siempre al modelo flash vigente, así que no debería quedar obsoleto nunca (a diferencia de
     * nombres de versión fija como "gemini-3.6-flash", que Google puede retirar en cualquier
     * momento, como ya pasó antes con 2.0 y 2.5). Los nombres fijos quedan como respaldo por si
     * el alias fallara por algún motivo puntual.
     */
    private val modelFallbackOrder = listOf(
        "gemini-flash-latest",
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.7-flash"
    )

    suspend fun estimateNutrition(
        foodPhotos: List<File>,
        labelPhotos: List<File>,
        userNote: String,
        context: String = "meal",
        overallTimeoutMs: Long = 30_000,
        onProgress: (String) -> Unit = {}
    ): GeminiResult = withContext(Dispatchers.IO) {
        val prompt = buildEstimatePrompt(userNote, labelPhotos.isNotEmpty(), foodPhotos.size)
        val parts = buildJsonArray {
            add(buildJsonObject { put("text", prompt) })
            foodPhotos.forEach { add(imagePart(it)) }
            labelPhotos.forEach { add(imagePart(it)) }
        }
        val requestBody = buildRequestBody(parts, jsonResponse = true)

        when (val result = fetchTextWithFallback(requestBody, context, overallTimeoutMs, onProgress)) {
            is TextFetchResult.Success -> parseEstimate(result.text)
            is TextFetchResult.Error -> GeminiResult.Error(result.message)
        }
    }

    /**
     * Le pide a Gemini sugerencias sobre el consumo del día (alimentos, efectos/beneficios),
     * a partir de un resumen textual ya armado por la UI (comidas, metas, gasto, peso, contexto
     * adicional del usuario). No usa imágenes.
     */
    suspend fun getDailyAdvice(
        prompt: String,
        context: String = "coach",
        overallTimeoutMs: Long = 30_000,
        onProgress: (String) -> Unit = {}
    ): CoachResult = withContext(Dispatchers.IO) {
        val parts = buildJsonArray { add(buildJsonObject { put("text", prompt) }) }
        val requestBody = buildRequestBody(parts, jsonResponse = false)

        when (val result = fetchTextWithFallback(requestBody, context, overallTimeoutMs, onProgress)) {
            is TextFetchResult.Success -> CoachResult.Success(result.text.trim())
            is TextFetchResult.Error -> CoachResult.Error(result.message)
        }
    }

    private fun buildRequestBody(parts: kotlinx.serialization.json.JsonElement, jsonResponse: Boolean): String =
        buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", parts)
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.3)
                if (jsonResponse) put("responseMimeType", "application/json")
            })
        }.toString()

    private suspend fun fetchTextWithFallback(
        requestBody: String,
        context: String,
        overallTimeoutMs: Long,
        onProgress: (String) -> Unit
    ): TextFetchResult {
        var lastError: TextFetchResult.Error? = null
        val triedModels = mutableSetOf<String>()
        // Límite global de espera: antes de esto, cada modelo agotaba sus propios reintentos
        // sin ningún tope conjunto, así que una racha de fallos podía dejar a la app "pensando"
        // varios minutos sin mostrar nada. Ahora, pasado este plazo, se corta y se reporta el
        // último error conocido en vez de seguir probando modelos en silencio.
        val deadline = System.currentTimeMillis() + overallTimeoutMs

        for (model in modelFallbackOrder) {
            if (System.currentTimeMillis() >= deadline) break
            triedModels += model
            onProgress("Probando $model...")
            val result = fetchModelText(model, requestBody, deadline)
            if (result is TextFetchResult.Success) return result
            lastError = result as TextFetchResult.Error
            onLog(GeminiAttemptLog(Instant.now(), context, model, result.message))
            // Solo se aborta de inmediato ante errores que ningún otro modelo va a resolver
            // (clave inválida, permisos, request malformado). Todo lo demás (modelo caído,
            // sobrecargado o removido) sigue probando el siguiente de la lista.
            if (isFatalError(result.message)) return result
        }

        // Si todos los modelos conocidos fallaron (ej. Google renombró/removió modelos otra vez),
        // se consulta el catálogo real de modelos disponibles y se prueba con el resto, siempre
        // que todavía quede tiempo dentro del límite global.
        if (System.currentTimeMillis() < deadline) {
            onProgress("Buscando modelos disponibles...")
            for (model in discoverFallbackModels(triedModels)) {
                if (System.currentTimeMillis() >= deadline) break
                triedModels += model
                onProgress("Probando $model...")
                val result = fetchModelText(model, requestBody, deadline)
                if (result is TextFetchResult.Success) return result
                lastError = result as TextFetchResult.Error
                onLog(GeminiAttemptLog(Instant.now(), context, model, result.message))
            }
        }

        val timeoutNote = if (System.currentTimeMillis() >= deadline) {
            " (se agotó el tiempo de espera de ${overallTimeoutMs / 1000}s probando modelos)"
        } else ""
        return lastError?.let { TextFetchResult.Error(it.message + timeoutNote) }
            ?: TextFetchResult.Error("No se pudo contactar a ningún modelo de Gemini$timeoutNote").also {
                onLog(GeminiAttemptLog(Instant.now(), context, null, it.message))
            }
    }

    /**
     * Nombres de versión que Google ya retiró para usuarios nuevos, comprobado en el campo
     * (siguen apareciendo en el catálogo de /v1beta/models con "generateContent" soportado,
     * pero responden 404 al llamarlos). El catálogo por sí solo no distingue "listado" de
     * "realmente disponible", así que se excluyen a mano en vez de perder tiempo probándolos.
     */
    private val knownRetiredVersionMarkers = listOf("-2.0-", "-2.5-", "-1.0-", "-1.5-")

    /** Extrae el número de versión de un nombre de modelo (ej. "gemini-3.5-flash" -> 3.5). */
    private fun extractVersion(name: String): Double =
        Regex("""gemini-(\d+(?:\.\d+)?)""").find(name)?.groupValues?.get(1)?.toDoubleOrNull() ?: -1.0

    private fun discoverFallbackModels(alreadyTried: Set<String>): List<String> {
        return try {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models")
                .addHeader("x-goog-api-key", apiKey)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyString = response.body?.string().orEmpty()
                val root = json.parseToJsonElement(bodyString) as? JsonObject ?: return emptyList()
                val models = root["models"] as? JsonArray ?: return emptyList()

                models.mapNotNull { it as? JsonObject }
                    .filter { model ->
                        val methods = (model["supportedGenerationMethods"] as? JsonArray)
                            ?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
                        "generateContent" in methods
                    }
                    .mapNotNull { model ->
                        (model["name"] as? JsonPrimitive)?.content?.removePrefix("models/")
                    }
                    .filter { name ->
                        "flash" in name.lowercase() && name !in alreadyTried &&
                            knownRetiredVersionMarkers.none { marker -> marker in name }
                    }
                    // El catálogo no viene ordenado por vigencia: se prueba primero el número de
                    // versión más alto (el "-latest" real, sin número, va al final de este orden,
                    // pero ya se intentó explícitamente antes vía modelFallbackOrder).
                    .sortedByDescending { name -> extractVersion(name) }
                    .take(3)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        /**
         * Errores que ningún reintento automático (ni en primer plano ni en segundo plano) va
         * a resolver por sí solo: requieren que el usuario corrija algo (ej. la API key). Se
         * expone públicamente para que la UI decida no reintentar/guardar como pendiente ante
         * este tipo de errores.
         */
        fun isFatalError(message: String): Boolean =
            message.contains("400") || message.contains("401") || message.contains("403") ||
                message.contains("API key", ignoreCase = true) ||
                message.contains("PERMISSION_DENIED", ignoreCase = true) ||
                message.contains("INVALID_ARGUMENT", ignoreCase = true)
    }

    private fun fetchModelText(model: String, requestBody: String, deadline: Long): TextFetchResult {
        // Un corte de red o un fallo de DNS puntual (muy común en 4G/5G real, aunque el
        // promedio de la conexión sea bueno: el resolver del teléfono a veces falla en un
        // intento aislado) no debería tirar todo el intento: se reintenta antes de pasar al
        // siguiente modelo o reportar el error. UnknownHostException también se reintenta,
        // con una pequeña pausa para darle tiempo al resolver DNS del sistema a recuperarse.
        // Se limita a 2 intentos (antes 3) para que el límite global de espera se respete con
        // margen, en vez de que un solo modelo agote casi todo el presupuesto de tiempo.
        val maxAttempts = 2
        repeat(maxAttempts) { attempt ->
            if (System.currentTimeMillis() >= deadline) return TextFetchResult.Error("Se agotó el tiempo de espera antes de completar el intento con $model")
            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return TextFetchResult.Error("Error de Gemini con $model (${response.code}): $bodyString")
                    }
                    return extractText(bodyString)
                }
            } catch (e: java.net.UnknownHostException) {
                if (attempt == maxAttempts - 1) {
                    // Se llega aquí incluso después de intentar resolver por DNS-over-HTTPS como
                    // respaldo (FallbackDns), así que ya no es un simple corte de red: apunta a
                    // que algo en la red específicamente bloquea este dominio.
                    return TextFetchResult.Error(
                        "No se pudo conectar con el servidor de Gemini (generativelanguage.googleapis.com) " +
                            "tras $maxAttempts intentos, incluso probando una resolución DNS alternativa. " +
                            "Es probable que tu red (operador, DNS privado o VPN) esté bloqueando ese dominio " +
                            "específico, no un problema general de tu internet. Prueba cambiar de WiFi a datos " +
                            "móviles (o viceversa), desactivar un DNS privado/VPN si tienes uno activo, y reintenta."
                    )
                }
                Thread.sleep(800L * (attempt + 1))
            } catch (e: java.io.IOException) {
                if (attempt == maxAttempts - 1) return TextFetchResult.Error("Fallo de conexión con $model tras reintentar: ${e.message}")
                Thread.sleep(500L * (attempt + 1))
            } catch (e: Exception) {
                return TextFetchResult.Error("No se pudo interpretar la respuesta de $model: ${e.message}")
            }
        }
        return TextFetchResult.Error("Fallo de conexión con $model")
    }

    private fun extractText(body: String): TextFetchResult {
        return try {
            val root = json.parseToJsonElement(body).let { it as JsonObject }
            val candidates = root["candidates"]?.let { it as JsonArray }
                ?: return TextFetchResult.Error("Respuesta sin candidatos: $body")
            val firstCandidate = candidates.firstOrNull() as? JsonObject
                ?: return TextFetchResult.Error("Candidato vacío: $body")
            val content = firstCandidate["content"] as? JsonObject
            val parts = content?.get("parts") as? JsonArray
            val text = (parts?.firstOrNull() as? JsonObject)?.get("text")
                ?.let { it as? JsonPrimitive }?.content
                ?: return TextFetchResult.Error("Sin texto en la respuesta: $body")
            TextFetchResult.Success(text)
        } catch (e: Exception) {
            TextFetchResult.Error("No se pudo interpretar la respuesta de Gemini: ${e.message}")
        }
    }

    private fun imagePart(file: File): JsonObject {
        val base64 = Base64.encodeToString(downscaleToJpeg(file), Base64.NO_WRAP)
        return buildJsonObject {
            put("inlineData", buildJsonObject {
                put("mimeType", "image/jpeg")
                put("data", base64)
            })
        }
    }

    /**
     * Las fotos de cámara pueden venir en resoluciones muy altas (12+ MP). Enviarlas tal cual a
     * Gemini infla el payload a varios MB por foto (más con varias fotos por comida), lo que
     * hace la subida lenta y frágil ante cualquier variación de red, y puede agotar la memoria
     * al decodificar varias a la vez. Gemini no necesita resolución completa para reconocer
     * comida, así que se reduce a un máximo de 1280px de lado antes de comprimir.
     */
    private fun downscaleToJpeg(file: File, maxDimension: Int = 1280, quality: Int = 80): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= maxDimension || bounds.outHeight / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IllegalStateException("No se pudo decodificar la imagen ${file.name}")

        val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (scale < 1f) {
            val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled
        }

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            bitmap.recycle()
            stream.toByteArray()
        }
    }

    private fun buildEstimatePrompt(userNote: String, hasLabelPhotos: Boolean, foodPhotoCount: Int): String {
        val notePart = if (userNote.isNotBlank()) {
            "El usuario describe el alimento así: \"$userNote\". Usa esta descripción para mejorar la estimación."
        } else {
            "El usuario no agregó descripción de texto."
        }
        val multiPhotoPart = if (foodPhotoCount > 1) {
            "Las primeras $foodPhotoCount imágenes son fotos de distintos platos o alimentos de una misma comida " +
                "(por ejemplo: entrada, plato de fondo, postre). Estímalos como parte de UNA sola comida y entrega " +
                "el total combinado de todos ellos, no solo del primero."
        } else ""
        val labelPart = if (hasLabelPhotos) {
            "También se incluyen una o más fotografías de la etiqueta de información nutricional del empaque; " +
                "prioriza esos datos exactos sobre tu estimación visual cuando estén disponibles y sean legibles."
        } else ""

        return """
            Eres un nutricionista experto. Analiza la(s) imagen(es) de comida entregada(s) y estima su contenido nutricional total.
            $notePart
            $multiPhotoPart
            $labelPart
            Además de la estimación nutricional, actúa como nutricionista educador: explica en términos simples
            los beneficios de lo detectado, sus aspectos menos beneficiosos, y si conviene consumirlo con
            frecuencia o no y por qué. El objetivo es que la persona vaya ganando conciencia sobre lo que come,
            así que sé concreto y evita generalidades vacías.
            Responde ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con este formato exacto:
            {
              "calories": <entero, kcal totales>,
              "proteinGrams": <número, gramos de proteína>,
              "carbsGrams": <número, gramos de carbohidratos>,
              "fatGrams": <número, gramos de grasa>,
              "detectedFoods": [<lista de strings con los alimentos detectados>],
              "confidenceNote": "<breve nota sobre la confianza o supuestos de la estimación, en español>",
              "benefits": "<1-3 frases sobre los aspectos nutricionales positivos de lo detectado, en español>",
              "drawbacks": "<1-3 frases sobre los aspectos menos beneficiosos o a moderar, en español>",
              "frequencyAdvice": "<1-2 frases indicando si conviene consumirlo frecuentemente o no, y por qué, en español>"
            }
        """.trimIndent()
    }

    private fun parseEstimate(text: String): GeminiResult {
        return try {
            val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val estimate = json.decodeFromString(NutritionEstimate.serializer(), cleaned)
            GeminiResult.Success(estimate)
        } catch (e: Exception) {
            GeminiResult.Error("No se pudo interpretar la respuesta de Gemini: ${e.message}")
        }
    }
}
