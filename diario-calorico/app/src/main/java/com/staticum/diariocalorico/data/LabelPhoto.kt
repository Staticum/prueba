package com.staticum.diariocalorico.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "label_photos",
    foreignKeys = [
        ForeignKey(
            entity = MealEntry::class,
            parentColumns = ["id"],
            childColumns = ["mealEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LabelPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealEntryId: Long,
    val photoPath: String
)
