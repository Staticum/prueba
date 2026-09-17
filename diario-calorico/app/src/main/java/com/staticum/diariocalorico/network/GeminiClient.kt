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
        val prompt = buildPrompt(userNote, labelPhotos.isNotEmpty(), foodPhotos.size)
        val parts = buildJsonArray {
            add(buildJsonObject { put("text", prompt) })
            foodPhotos.forEach { add(imagePart(it)) }
            labelPhotos.forEach { add(imagePart(it)) }
        }
        val requestBody = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", parts)
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            })
        }.toString()

        var lastError: GeminiResult.Error? = null
        val triedModels = mutableSetOf<String>()

        for (model in modelFallbackOrder) {
            triedModels += model
            val result = callModel(model, requestBody)
            if (result is GeminiResult.Success) return@withContext result
            lastError = result as GeminiResult.Error
            // Solo se aborta de inmediato ante errores que ningún otro modelo va a resolver
            // (clave inválida, permisos, request malformado). Todo lo demás (modelo caído,
            // sobrecargado o removido) sigue probando el siguiente de la lista.
            if (isFatalError(result.message)) return@withContext result
        }

        // Si todos los modelos conocidos fallaron (ej. Google renombró/removió modelos otra vez),
        // se consulta el catálogo real de modelos disponibles y se prueba con el resto.
        for (model in discoverFallbackModels(triedModels)) {
            triedModels += model
            val result = callModel(model, requestBody)
            if (result is GeminiResult.Success) return@withContext result
            lastError = result as GeminiResult.Error
        }

        lastError ?: GeminiResult.Error("No se pudo contactar a ningún modelo de Gemini")
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
            message.contains("INVALID_ARGUMENT", ignoreCase = true)

    private fun callModel(model: String, requestBody: String): GeminiResult {
        return try {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return GeminiResult.Error("Error de Gemini con $model (${response.code}): $bodyString")
                }
                parseResponse(bodyString)
            }
        } catch (e: Exception) {
            GeminiResult.Error("Fallo de red o parseo con $model: ${e.message}")
        }
    }

    private fun imagePart(file: File): JsonObject {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val bytes = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        }
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return buildJsonObject {
            put("inlineData", buildJsonObject {
                put("mimeType", "image/jpeg")
                put("data", base64)
            })
        }
    }

    private fun buildPrompt(userNote: String, hasLabelPhotos: Boolean, foodPhotoCount: Int): String {
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

    private fun parseResponse(body: String): GeminiResult {
        return try {
            val root = json.parseToJsonElement(body).let { it as JsonObject }
            val candidates = root["candidates"]?.let { it as JsonArray }
                ?: return GeminiResult.Error("Respuesta sin candidatos: $body")
            val firstCandidate = candidates.firstOrNull() as? JsonObject
                ?: return GeminiResult.Error("Candidato vacío: $body")
            val content = firstCandidate["content"] as? JsonObject
            val parts = content?.get("parts") as? JsonArray
            val text = (parts?.firstOrNull() as? JsonObject)?.get("text")
                ?.let { it as? JsonPrimitive }?.content
                ?: return GeminiResult.Error("Sin texto en la respuesta: $body")

            val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val estimate = json.decodeFromString(NutritionEstimate.serializer(), cleaned)
            GeminiResult.Success(estimate)
        } catch (e: Exception) {
            GeminiResult.Error("No se pudo interpretar la respuesta de Gemini: ${e.message}")
        }
    }
}
