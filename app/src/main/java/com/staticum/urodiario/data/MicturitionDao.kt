package com.staticum.urodiario.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MicturitionDao {

    @Transaction
    @Query("SELECT * FROM micturition_records ORDER BY dateTimeMillis DESC")
    fun observeAllWithPhotos(): Flow<List<RecordWithPhotos>>

    @Transaction
    @Query("SELECT * FROM micturition_records WHERE id = :recordId")
    fun observeByIdWithPhotos(recordId: Long): Flow<RecordWithPhotos?>

    @Transaction
    @Query("SELECT * FROM micturition_records WHERE dateTimeMillis BETWEEN :startMillis AND :endMillis ORDER BY dateTimeMillis DESC")
    fun observeBetweenWithPhotos(startMillis: Long, endMillis: Long): Flow<List<RecordWithPhotos>>

    @Query("SELECT * FROM micturition_records ORDER BY dateTimeMillis ASC")
    suspend fun getAllForExport(): List<MicturitionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MicturitionRecord): Long

    @Update
    suspend fun updateRecord(record: MicturitionRecord)

    @Delete
    suspend fun deleteRecord(record: MicturitionRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: RecordPhoto): Long

    @Delete
    suspend fun deletePhoto(photo: RecordPhoto)

    @Query("SELECT * FROM record_photos WHERE recordId = :recordId")
    suspend fun getPhotosForRecord(recordId: Long): List<RecordPhoto>
}
