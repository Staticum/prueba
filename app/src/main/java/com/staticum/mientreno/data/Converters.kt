package com.staticum.mientreno.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromCategory(value: ExerciseCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): ExerciseCategory =
        runCatching { ExerciseCategory.valueOf(value) }.getOrDefault(ExerciseCategory.OTRO)

    @TypeConverter
    fun fromMeasureType(value: MeasureType): String = value.name

    @TypeConverter
    fun toMeasureType(value: String): MeasureType =
        runCatching { MeasureType.valueOf(value) }.getOrDefault(MeasureType.REPS)
}
