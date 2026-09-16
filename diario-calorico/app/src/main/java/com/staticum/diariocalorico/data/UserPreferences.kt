package com.staticum.diariocalorico.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "diario_calorico_prefs")

data class DailyGoals(
    val calories: Int = 2000,
    val proteinGrams: Int = 120,
    val carbsGrams: Int = 250,
    val fatGrams: Int = 65
)

class UserPreferences(private val context: Context) {

    private object Keys {
        val CALORIES = intPreferencesKey("goal_calories")
        val PROTEIN = intPreferencesKey("goal_protein")
        val CARBS = intPreferencesKey("goal_carbs")
        val FAT = intPreferencesKey("goal_fat")
    }

    val dailyGoals: Flow<DailyGoals> = context.dataStore.data.map { prefs ->
        DailyGoals(
            calories = prefs[Keys.CALORIES] ?: 2000,
            proteinGrams = prefs[Keys.PROTEIN] ?: 120,
            carbsGrams = prefs[Keys.CARBS] ?: 250,
            fatGrams = prefs[Keys.FAT] ?: 65
        )
    }

    suspend fun setDailyGoals(goals: DailyGoals) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CALORIES] = goals.calories
            prefs[Keys.PROTEIN] = goals.proteinGrams
            prefs[Keys.CARBS] = goals.carbsGrams
            prefs[Keys.FAT] = goals.fatGrams
        }
    }

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "diario_calorico_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getGeminiApiKey(): String? = encryptedPrefs.getString(KEY_GEMINI_API, null)

    fun setGeminiApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_GEMINI_API, key).apply()
    }

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
    }
}
