package com.tsoft.audiotranscribe.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "speaker_names", primaryKeys = ["transcriptId", "speakerLabel"])
data class SpeakerNameEntity(
    val transcriptId: String,
    val speakerLabel: String,
    val displayName: String
)

@Dao
interface SpeakerNameDao {
    @Query("SELECT * FROM speaker_names WHERE transcriptId = :transcriptId")
    suspend fun getForTranscript(transcriptId: String): List<SpeakerNameEntity>

    @Upsert
    suspend fun upsert(entity: SpeakerNameEntity)
}

@Database(entities = [SpeakerNameEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun speakerNameDao(): SpeakerNameDao
}
