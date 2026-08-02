package com.staticum.urodiario.data

import kotlinx.coroutines.flow.Flow

class MicturitionRepository(private val dao: MicturitionDao) {

    fun observeAll(): Flow<List<RecordWithPhotos>> = dao.observeAllWithPhotos()

    fun observeById(recordId: Long): Flow<RecordWithPhotos?> = dao.observeByIdWithPhotos(recordId)

    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<RecordWithPhotos>> =
        dao.observeBetweenWithPhotos(startMillis, endMillis)

    suspend fun getAllForExport(): List<MicturitionRecord> = dao.getAllForExport()

    suspend fun saveRecord(record: MicturitionRecord, photoUris: List<String>): Long {
        val recordId = dao.insertRecord(record)
        val effectiveId = if (record.id != 0L) record.id else recordId
        photoUris.forEach { uri ->
            dao.insertPhoto(RecordPhoto(recordId = effectiveId, uri = uri))
        }
        return effectiveId
    }

    suspend fun deleteRecord(record: MicturitionRecord) = dao.deleteRecord(record)

    suspend fun addPhoto(recordId: Long, uri: String) {
        dao.insertPhoto(RecordPhoto(recordId = recordId, uri = uri))
    }

    suspend fun deletePhoto(photo: RecordPhoto) = dao.deletePhoto(photo)
}
