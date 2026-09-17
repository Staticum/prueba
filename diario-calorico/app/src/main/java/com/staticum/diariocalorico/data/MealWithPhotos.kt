package com.staticum.diariocalorico.data

import androidx.room.Embedded
import androidx.room.Relation

data class MealWithPhotos(
    @Embedded val meal: MealEntry,
    @Relation(parentColumn = "id", entityColumn = "mealEntryId")
    val foodPhotos: List<FoodPhoto>,
    @Relation(parentColumn = "id", entityColumn = "mealEntryId")
    val labelPhotos: List<LabelPhoto>
) {
    /** Todas las fotos de alimento de esta comida, con la principal (histórica) primero. */
    val allFoodPhotoPaths: List<String>
        get() = (listOf(meal.foodPhotoPath) + foodPhotos.map { it.photoPath }).distinct()
}
