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
    fun observeRoutineWithExercises(routineId: Long): Flow<RoutineWithExercises?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineTemplate): Long

    @Update
    suspend fun updateRoutine(routine: RoutineTemplate)

    @Delete
    suspend fun deleteRoutine(routine: RoutineTemplate)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesForRoutine(routineId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExercise>)

    @Transaction
    suspend fun replaceRoutineExercises(routineId: Long, exercises: List<RoutineExercise>) {
        deleteExercisesForRoutine(routineId)
        insertRoutineExercises(exercises)
    }
}
