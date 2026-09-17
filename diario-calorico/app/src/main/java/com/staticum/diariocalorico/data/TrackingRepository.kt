package com.staticum.diariocalorico.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class TrackingRepository(private val dao: TrackingDao) {

    suspend fun saveExpenditure(date: LocalDate, caloriesBurned: Int) =
        dao.upsertExpenditure(DailyExpenditure(date, caloriesBurned))

    suspend fun getExpenditureForDate(date: LocalDate): DailyExpenditure? = dao.getExpenditureForDate(date)

    fun observeExpenditureForDate(date: LocalDate): Flow<DailyExpenditure?> = dao.observeExpenditureForDate(date)

    suspend fun getExpenditureBetween(start: LocalDate, end: LocalDate): List<DailyExpenditure> =
        dao.getExpenditureBetween(start, end)

    suspend fun saveWeight(weightKg: Double, recordedAt: Instant = Instant.now()) =
        dao.insertWeight(WeightEntry(recordedAt = recordedAt, weightKg = weightKg))

    suspend fun ensureSeedWeight(defaultKg: Double) {
        if (dao.countWeights() == 0) saveWeight(defaultKg)
    }

    fun observeLatestWeight(): Flow<WeightEntry?> = dao.observeLatestWeight()

    suspend fun getWeightsBetween(start: Instant, end: Instant): List<WeightEntry> =
        dao.getWeightsBetween(start, end)
}
