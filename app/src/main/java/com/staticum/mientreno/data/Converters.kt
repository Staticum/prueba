package com.staticum.mientreno.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCategory(value: ExerciseCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): ExerciseCategory =
        runCatching { ExerciseCategory.valueOf(value) }.getOrDefault(ExerciseCategory.OTRO)

    @TypeConverter
    fun fromType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toType(value: String): ExerciseType =
        runCatching { ExerciseType.valueOf(value) }.getOrDefault(ExerciseType.FUERZA)
}
