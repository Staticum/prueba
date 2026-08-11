package com.staticum.mientreno.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routine_templates ORDER BY name")
    fun observeAllRoutines(): Flow<List<RoutineTemplate>>

    @Transaction
    @Query("SELECT * FROM routine_templates WHERE id = :routineId")
    fun observeRoutineWithBlocks(routineId: Long): Flow<RoutineWithBlocks?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineTemplate): Long

    @Update
    suspend fun updateRoutine(routine: RoutineTemplate)

    @Delete
    suspend fun deleteRoutine(routine: RoutineTemplate)

    @Query("SELECT id FROM routine_blocks WHERE routineId = :routineId")
    suspend fun getBlockIdsForRoutine(routineId: Long): List<Long>

    @Query("DELETE FROM routine_exercises WHERE blockId IN (:blockIds)")
    suspend fun deleteExercisesForBlocks(blockIds: List<Long>)

    @Query("DELETE FROM routine_blocks WHERE routineId = :routineId")
    suspend fun deleteBlocksForRoutine(routineId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: RoutineBlock): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<RoutineExercise>)

    @Transaction
    suspend fun replaceRoutineBlocks(routineId: Long, blocks: List<Pair<RoutineBlock, List<RoutineExercise>>>) {
        val existingBlockIds = getBlockIdsForRoutine(routineId)
        if (existingBlockIds.isNotEmpty()) {
            deleteExercisesForBlocks(existingBlockIds)
        }
        deleteBlocksForRoutine(routineId)

        blocks.forEach { (block, exercises) ->
            val blockId = insertBlock(block)
            if (exercises.isNotEmpty()) {
                insertExercises(exercises.map { it.copy(blockId = blockId) })
            }
        }
    }
}
