package com.staticum.diariocalorico

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.staticum.diariocalorico.data.AppDatabase
import com.staticum.diariocalorico.data.GeminiLogRepository
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.TrackingRepository
import com.staticum.diariocalorico.data.UserPreferences
import com.staticum.diariocalorico.work.GeminiRetryWorker
import java.util.concurrent.TimeUnit

class DiarioCaloricoApplication : Application() {
    val repository: MealRepository by lazy {
        MealRepository(AppDatabase.getInstance(this).mealDao())
    }
    val trackingRepository: TrackingRepository by lazy {
        TrackingRepository(AppDatabase.getInstance(this).trackingDao())
    }
    val geminiLogRepository: GeminiLogRepository by lazy {
        GeminiLogRepository(AppDatabase.getInstance(this).geminiLogDao())
    }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        schedulePendingAnalysisRetry()
    }

    /**
     * Reintenta cada 2 horas, en segundo plano, el análisis de las comidas que quedaron
     * pendientes porque Gemini no respondió a tiempo — sin que la persona tenga que abrir la
     * app y reintentar manualmente.
     */
    private fun schedulePendingAnalysisRetry() {
        val request = PeriodicWorkRequestBuilder<GeminiRetryWorker>(2, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "gemini_pending_analysis_retry",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
