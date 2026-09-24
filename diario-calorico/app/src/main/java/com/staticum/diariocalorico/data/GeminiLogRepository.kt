package com.staticum.diariocalorico.data

import com.staticum.diariocalorico.network.GeminiAttemptLog

class GeminiLogRepository(private val dao: GeminiLogDao) {
    suspend fun log(entry: GeminiLogEntry) {
        dao.insert(entry)
        dao.trimToLatest()
    }

    suspend fun log(attempt: GeminiAttemptLog) {
        log(GeminiLogEntry(timestamp = attempt.timestamp, context = attempt.context, model = attempt.model, message = attempt.message))
    }

    suspend fun getAll(): List<GeminiLogEntry> = dao.getAll()
}
