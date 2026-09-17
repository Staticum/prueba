package com.staticum.diariocalorico

import android.app.Application
import com.staticum.diariocalorico.data.AppDatabase
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
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
}
