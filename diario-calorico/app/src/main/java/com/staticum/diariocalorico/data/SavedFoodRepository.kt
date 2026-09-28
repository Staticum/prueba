package com.staticum.diariocalorico.data

import kotlinx.coroutines.flow.Flow

class SavedFoodRepository(private val dao: SavedFoodDao) {
    fun observeAll(): Flow<List<SavedFood>> = dao.observeAll()
    suspend fun getAll(): List<SavedFood> = dao.getAll()
    suspend fun save(food: SavedFood): Long = dao.insert(food)
    suspend fun update(food: SavedFood) = dao.update(food)
    suspend fun delete(food: SavedFood) = dao.delete(food)
}
