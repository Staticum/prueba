package com.staticum.diariocalorico.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val consumedAt: Instant,
    val mealType: MealType,
    val description: String,
    val foodPhotoPath: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val detectedFoods: String,
    val createdAt: Instant = Instant.now()
)
