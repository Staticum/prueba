package com.staticum.urodiario.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromUrineColor(value: UrineColor): String = value.name

    @TypeConverter
    fun toUrineColor(value: String): UrineColor =
        runCatching { UrineColor.valueOf(value) }.getOrDefault(UrineColor.OTRO)

    @TypeConverter
    fun fromUrineOdor(value: UrineOdor): String = value.name

    @TypeConverter
    fun toUrineOdor(value: String): UrineOdor =
        runCatching { UrineOdor.valueOf(value) }.getOrDefault(UrineOdor.OTRO)
}
