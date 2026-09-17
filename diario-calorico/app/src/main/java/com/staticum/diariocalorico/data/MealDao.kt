package com.staticum.diariocalorico.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealEntry): Long

    @Update
    suspend fun updateMeal(meal: MealEntry)

    @Insert
    suspend fun insertFoodPhotos(photos: List<FoodPhoto>)

    @Insert
    suspend fun insertLabelPhotos(photos: List<LabelPhoto>)

    @Query("DELETE FROM food_photos WHERE mealEntryId = :mealId")
    suspend fun deleteFoodPhotosForMeal(mealId: Long)

    @Query("DELETE FROM label_photos WHERE mealEntryId = :mealId")
    suspend fun deleteLabelPhotosForMeal(mealId: Long)

    @Delete
    suspend fun deleteMeal(meal: MealEntry)

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE id = :id")
    suspend fun getMealWithPhotos(id: Long): MealWithPhotos?

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE consumedAt BETWEEN :start AND :end ORDER BY consumedAt DESC")
    fun observeMealsBetween(start: Instant, end: Instant): Flow<List<MealWithPhotos>>

    @Transaction
    @Query("SELECT * FROM meal_entries ORDER BY consumedAt DESC")
    fun observeAllMeals(): Flow<List<MealWithPhotos>>

    @Query("SELECT * FROM meal_entries WHERE consumedAt BETWEEN :start AND :end ORDER BY consumedAt DESC")
    suspend fun getMealsBetween(start: Instant, end: Instant): List<MealEntry>

    @Query(
        """
        SELECT * FROM meal_entries
        GROUP BY detectedFoods
        ORDER BY COUNT(*) DESC, MAX(consumedAt) DESC
        LIMIT 8
        """
    )
    suspend fun getFrequentMeals(): List<MealEntry>
}
