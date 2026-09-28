package com.staticum.diariocalorico.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFoodDao {
    @Insert
    suspend fun insert(food: SavedFood): Long

    @Update
    suspend fun update(food: SavedFood)

    @Delete
    suspend fun delete(food: SavedFood)

    @Query("SELECT * FROM saved_foods ORDER BY name ASC")
    fun observeAll(): Flow<List<SavedFood>>

    @Query("SELECT * FROM saved_foods ORDER BY name ASC")
    suspend fun getAll(): List<SavedFood>
}
