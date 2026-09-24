package com.staticum.diariocalorico.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Registro de cada fallo de Gemini (por modelo/intento), para poder diagnosticar después
 * problemas de conectividad recurrentes en vez de solo mostrar un error puntual en pantalla.
 */
@Entity(tableName = "gemini_log")
data class GeminiLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val context: String,
    val model: String?,
    val message: String
)
