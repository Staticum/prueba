package com.staticum.diariocalorico.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GeminiLogDao {
    @Insert
    suspend fun insert(entry: GeminiLogEntry)

    @Query("SELECT * FROM gemini_log ORDER BY timestamp DESC")
    suspend fun getAll(): List<GeminiLogEntry>

    // Evita que el log crezca sin límite ante reintentos indefinidos: conserva solo los más recientes.
    @Query("DELETE FROM gemini_log WHERE id NOT IN (SELECT id FROM gemini_log ORDER BY timestamp DESC LIMIT 500)")
    suspend fun trimToLatest()
}
