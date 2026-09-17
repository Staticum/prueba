package com.staticum.diariocalorico.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface TrackingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpenditure(entry: DailyExpenditure)

    @Query("SELECT * FROM daily_expenditure WHERE date = :date")
    suspend fun getExpenditureForDate(date: LocalDate): DailyExpenditure?

    @Query("SELECT * FROM daily_expenditure WHERE date = :date")
    fun observeExpenditureForDate(date: LocalDate): Flow<DailyExpenditure?>

    @Query("SELECT * FROM daily_expenditure WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getExpenditureBetween(start: LocalDate, end: LocalDate): List<DailyExpenditure>

    @Insert
    suspend fun insertWeight(entry: WeightEntry)

    @Query("SELECT COUNT(*) FROM weight_entries")
    suspend fun countWeights(): Int

    @Query("SELECT * FROM weight_entries ORDER BY recordedAt DESC LIMIT 1")
    fun observeLatestWeight(): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries WHERE recordedAt BETWEEN :start AND :end ORDER BY recordedAt ASC")
    suspend fun getWeightsBetween(start: Instant, end: Instant): List<WeightEntry>
}
