package com.staticum.diariocalorico.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant

class MealRepository(private val dao: MealDao) {

    suspend fun saveMeal(meal: MealEntry, labelPhotoPaths: List<String>): Long {
        val id = dao.insertMeal(meal)
        if (labelPhotoPaths.isNotEmpty()) {
            dao.insertLabelPhotos(labelPhotoPaths.map { LabelPhoto(mealEntryId = id, photoPath = it) })
        }
        return id
    }

    suspend fun deleteMeal(meal: MealEntry) = dao.deleteMeal(meal)

    fun observeMealsBetween(start: Instant, end: Instant): Flow<List<MealWithPhotos>> =
        dao.observeMealsBetween(start, end)

    fun observeAllMeals(): Flow<List<MealWithPhotos>> = dao.observeAllMeals()

    suspend fun getMealsBetween(start: Instant, end: Instant): List<MealEntry> =
        dao.getMealsBetween(start, end)
}
