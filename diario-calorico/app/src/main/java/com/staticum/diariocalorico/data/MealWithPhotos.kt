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
    /**
     * Todas las fotos de alimento de esta comida, con la principal (histórica) primero.
     * Las comidas registradas desde un alimento guardado no tienen foto (foodPhotoPath vacío).
     */
    val allFoodPhotoPaths: List<String>
        get() = (listOf(meal.foodPhotoPath) + foodPhotos.map { it.photoPath }).distinct().filter { it.isNotBlank() }
}
