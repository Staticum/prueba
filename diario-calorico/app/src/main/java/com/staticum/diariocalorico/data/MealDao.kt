package com.staticum.diariocalorico.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealEntry): Long

    @Insert
    suspend fun insertLabelPhotos(photos: List<LabelPhoto>)

    @Delete
    suspend fun deleteMeal(meal: MealEntry)

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE consumedAt BETWEEN :start AND :end ORDER BY consumedAt DESC")
    fun observeMealsBetween(start: Instant, end: Instant): Flow<List<MealWithPhotos>>

    @Transaction
    @Query("SELECT * FROM meal_entries ORDER BY consumedAt DESC")
    fun observeAllMeals(): Flow<List<MealWithPhotos>>

    @Query("SELECT * FROM meal_entries WHERE consumedAt BETWEEN :start AND :end ORDER BY consumedAt DESC")
    suspend fun getMealsBetween(start: Instant, end: Instant): List<MealEntry>
}
