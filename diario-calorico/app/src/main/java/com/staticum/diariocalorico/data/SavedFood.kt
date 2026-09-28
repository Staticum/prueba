package com.staticum.diariocalorico.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Alimento guardado por la persona (ej. "Batido creatina/aminoácidos", "Yogurt con cereales"),
 * con macros fijos, para registrarlo sin foto ni volver a llamar a Gemini cada vez que se repite.
 */
@Entity(tableName = "saved_foods")
data class SavedFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val detectedFoods: String
)
