package com.staticum.diariocalorico.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant

class MealRepository(private val dao: MealDao) {

    suspend fun saveMeal(meal: MealEntry, extraFoodPhotoPaths: List<String>, labelPhotoPaths: List<String>): Long {
        val id = dao.insertMeal(meal)
        if (extraFoodPhotoPaths.isNotEmpty()) {
            dao.insertFoodPhotos(extraFoodPhotoPaths.map { FoodPhoto(mealEntryId = id, photoPath = it) })
        }
        if (labelPhotoPaths.isNotEmpty()) {
            dao.insertLabelPhotos(labelPhotoPaths.map { LabelPhoto(mealEntryId = id, photoPath = it) })
        }
        return id
    }

    suspend fun updateMeal(meal: MealEntry, extraFoodPhotoPaths: List<String>, labelPhotoPaths: List<String>) {
        dao.updateMeal(meal)
        dao.deleteFoodPhotosForMeal(meal.id)
        dao.deleteLabelPhotosForMeal(meal.id)
        if (extraFoodPhotoPaths.isNotEmpty()) {
            dao.insertFoodPhotos(extraFoodPhotoPaths.map { FoodPhoto(mealEntryId = meal.id, photoPath = it) })
        }
        if (labelPhotoPaths.isNotEmpty()) {
            dao.insertLabelPhotos(labelPhotoPaths.map { LabelPhoto(mealEntryId = meal.id, photoPath = it) })
        }
    }

    suspend fun getMealWithPhotos(id: Long): MealWithPhotos? = dao.getMealWithPhotos(id)

    suspend fun deleteMeal(meal: MealEntry) = dao.deleteMeal(meal)

    fun observeMealsBetween(start: Instant, end: Instant): Flow<List<MealWithPhotos>> =
        dao.observeMealsBetween(start, end)

    fun observeAllMeals(): Flow<List<MealWithPhotos>> = dao.observeAllMeals()

    suspend fun getMealsBetween(start: Instant, end: Instant): List<MealEntry> =
        dao.getMealsBetween(start, end)

    suspend fun getFrequentMeals(): List<MealEntry> = dao.getFrequentMeals()

    suspend fun getPendingAnalysisMeals(): List<MealEntry> = dao.getPendingAnalysisMeals()
}
