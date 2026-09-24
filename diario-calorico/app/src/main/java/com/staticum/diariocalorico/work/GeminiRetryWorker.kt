package com.staticum.diariocalorico.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.staticum.diariocalorico.R
import com.staticum.diariocalorico.data.AppDatabase
import com.staticum.diariocalorico.data.GeminiLogRepository
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.UserPreferences
import com.staticum.diariocalorico.network.GeminiClient
import com.staticum.diariocalorico.network.GeminiResult
import java.io.File

/**
 * Reintenta en segundo plano el análisis de las comidas guardadas como "pendientes" (Gemini no
 * respondió a tiempo cuando se agregaron). Se ejecuta cada 2 horas mientras haya conexión; el
 * usuario no necesita abrir la app ni reintentar manualmente.
 */
class GeminiRetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userPreferences = UserPreferences(applicationContext)
        val apiKey = userPreferences.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) return Result.success()

        val db = AppDatabase.getInstance(applicationContext)
        val mealRepository = MealRepository(db.mealDao())
        val geminiLogRepository = GeminiLogRepository(db.geminiLogDao())

        val pending = mealRepository.getPendingAnalysisMeals()
        if (pending.isEmpty()) return Result.success()

        val client = GeminiClient(apiKey, onLog = { entry -> geminiLogRepository.log(entry) })
        var resolvedCount = 0

        for (meal in pending) {
            val withPhotos = mealRepository.getMealWithPhotos(meal.id) ?: continue
            val foodPhotos = withPhotos.allFoodPhotoPaths.map { File(it) }.filter { it.exists() }
            val labelPhotos = withPhotos.labelPhotos.map { File(it.photoPath) }.filter { it.exists() }
            if (foodPhotos.isEmpty()) continue

            val result = client.estimateNutrition(
                foodPhotos, labelPhotos, meal.description,
                context = "background-retry:${meal.id}",
                overallTimeoutMs = 90_000
            )
            if (result is GeminiResult.Success) {
                val estimate = result.estimate
                mealRepository.updateMeal(
                    meal.copy(
                        calories = estimate.calories,
                        proteinGrams = estimate.proteinGrams,
                        carbsGrams = estimate.carbsGrams,
                        fatGrams = estimate.fatGrams,
                        detectedFoods = estimate.detectedFoods.joinToString(", "),
                        analysisPending = false
                    ),
                    emptyList(),
                    emptyList()
                )
                resolvedCount++
            }
        }

        if (resolvedCount > 0) notifyResolved(resolvedCount)
        return Result.success()
    }

    private fun notifyResolved(count: Int) {
        val channelId = "gemini_pending_analysis"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Análisis pendientes de Gemini", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val text = if (count == 1) {
            "Se completó el análisis de 1 comida que había quedado pendiente."
        } else {
            "Se completó el análisis de $count comidas que habían quedado pendientes."
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Diario Calórico")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
