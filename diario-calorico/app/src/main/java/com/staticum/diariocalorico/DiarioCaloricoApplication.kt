package com.staticum.diariocalorico

import android.app.Application
import androidx.work.WorkManager
import com.staticum.diariocalorico.data.AppDatabase
import com.staticum.diariocalorico.data.GeminiLogRepository
import com.staticum.diariocalorico.data.MealRepository
import com.staticum.diariocalorico.data.TrackingRepository
import com.staticum.diariocalorico.data.UserPreferences

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
        // El reintento periódico automático (cada 2h) se eliminó: con la cuota gratuita de
        // Gemini tan ajustada (20 solicitudes/minuto en algunos modelos), un proceso corriendo
        // solo, sin que la persona lo note, terminaba agotándola. El análisis de comidas
        // pendientes ahora solo se reintenta bajo demanda, desde Ajustes > "Reintentar análisis
        // pendientes ahora". Se cancela cualquier trabajo periódico que haya quedado programado
        // de una versión anterior de la app.
        WorkManager.getInstance(this).cancelUniqueWork("gemini_pending_analysis_retry")
    }
}
