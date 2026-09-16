package com.staticum.diariocalorico.data

import androidx.room.Embedded
import androidx.room.Relation

data class MealWithPhotos(
    @Embedded val meal: MealEntry,
    @Relation(parentColumn = "id", entityColumn = "mealEntryId")
    val labelPhotos: List<LabelPhoto>
)
