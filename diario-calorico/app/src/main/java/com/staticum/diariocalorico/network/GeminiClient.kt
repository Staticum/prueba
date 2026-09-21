package com.staticum.diariocalorico.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

private sealed class TextFetchResult {
    data class Success(val text: String) : TextFetchResult()
    data class Error(val message: String) : TextFetchResult()
}

class GeminiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Orden de modelos a intentar. Si uno responde con error transitorio (503/429, sobrecarga
     * o modelo no disponible) se reintenta con el siguiente antes de reportar fallo al usuario.
     */
    private val modelFallbackOrder = listOf(
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.7-flash",
        "gemini-flash-latest"
    )

    suspend fun estimateNutrition(
        foodPhotos: List<File>,
        labelPhotos: List<File>,
        userNote: String
    ): GeminiResult = withContext(Dispatchers.IO) {
        val prompt = buildEstimatePrompt(userNote, labelPhotos.isNotEmpty(), foodPhotos.size)
        val parts = buildJsonArray {
            add(buildJsonObject { put("text", prompt) })
            foodPhotos.forEach { add(imagePart(it)) }
            labelPhotos.forEach { add(imagePart(it)) }
        }
        val requestBody = buildRequestBody(parts, jsonResponse = true)

        when (val result = fetchTextWithFallback(requestBody)) {
            is TextFetchResult.Success -> parseEstimate(result.text)
            is TextFetchResult.Error -> GeminiResult.Error(result.message)
        }
    }

    /**
     * Le pide a Gemini sugerencias sobre el consumo del día (alimentos, efectos/beneficios),
     * a partir de un resumen textual ya armado por la UI (comidas, metas, gasto, peso, contexto
     * adicional del usuario). No usa imágenes.
     */
    suspend fun getDailyAdvice(prompt: String): CoachResult = withContext(Dispatchers.IO) {
        val parts = buildJsonArray { add(buildJsonObject { put("text", prompt) }) }
        val requestBody = buildRequestBody(parts, jsonResponse = false)

        when (val result = fetchTextWithFallback(requestBody)) {
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

    private fun fetchTextWithFallback(requestBody: String): TextFetchResult {
        var lastError: TextFetchResult.Error? = null
        val triedModels = mutableSetOf<String>()

        for (model in modelFallbackOrder) {
            triedModels += model
            val result = fetchModelText(model, requestBody)
            if (result is TextFetchResult.Success) return result
            lastError = result as TextFetchResult.Error
            // Solo se aborta de inmediato ante errores que ningún otro modelo va a resolver
            // (clave inválida, permisos, request malformado). Todo lo demás (modelo caído,
            // sobrecargado o removido) sigue probando el siguiente de la lista.
            if (isFatalError(result.message)) return result
        }

        // Si todos los modelos conocidos fallaron (ej. Google renombró/removió modelos otra vez),
        // se consulta el catálogo real de modelos disponibles y se prueba con el resto.
        for (model in discoverFallbackModels(triedModels)) {
            triedModels += model
            val result = fetchModelText(model, requestBody)
            if (result is TextFetchResult.Success) return result
            lastError = result as TextFetchResult.Error
        }

        return lastError ?: TextFetchResult.Error("No se pudo contactar a ningún modelo de Gemini")
    }

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
                    .filter { name -> "flash" in name.lowercase() && name !in alreadyTried }
                    .take(3)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isFatalError(message: String): Boolean =
        message.contains("400") || message.contains("401") || message.contains("403") ||
            message.contains("API key", ignoreCase = true) ||
            message.contains("PERMISSION_DENIED", ignoreCase = true) ||
            message.contains("INVALID_ARGUMENT", ignoreCase = true) ||
            message.contains("No hay conexión a internet")

    private fun fetchModelText(model: String, requestBody: String): TextFetchResult {
        // Un corte de red o un fallo de DNS puntual (muy común en 4G/5G real, aunque el
        // promedio de la conexión sea bueno: el resolver del teléfono a veces falla en un
        // intento aislado) no debería tirar todo el intento: se reintenta antes de pasar al
        // siguiente modelo o reportar el error. UnknownHostException también se reintenta,
        // con una pequeña pausa para darle tiempo al resolver DNS del sistema a recuperarse.
        val maxAttempts = 3
        repeat(maxAttempts) { attempt ->
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
                    return TextFetchResult.Error("No hay conexión a internet: no se pudo resolver el servidor de Gemini tras $maxAttempts intentos. Revisa tu WiFi o datos móviles e inténtalo de nuevo.")
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
            Responde ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con este formato exacto:
            {
              "calories": <entero, kcal totales>,
              "proteinGrams": <número, gramos de proteína>,
              "carbsGrams": <número, gramos de carbohidratos>,
              "fatGrams": <número, gramos de grasa>,
              "detectedFoods": [<lista de strings con los alimentos detectados>],
              "confidenceNote": "<breve nota sobre la confianza o supuestos de la estimación, en español>"
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
