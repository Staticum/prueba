package com.staticum.diariocalorico.network

import kotlinx.serialization.Serializable

@Serializable
data class NutritionEstimate(
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val detectedFoods: List<String>,
    val confidenceNote: String = ""
)

sealed class GeminiResult {
    data class Success(val estimate: NutritionEstimate) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}
