package com.staticum.mientreno.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM workout_sessions ORDER BY startMillis DESC")
    fun observeAllSessions(): Flow<List<WorkoutSession>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun observeSessionWithLogs(sessionId: Long): Flow<SessionWithLogs?>

    @Query("SELECT * FROM workout_sessions WHERE startMillis BETWEEN :startMillis AND :endMillis ORDER BY startMillis")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<WorkoutSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<SessionExerciseLog>)

    @Query("SELECT * FROM session_exercise_logs WHERE sessionId = :sessionId")
    suspend fun getLogsForSession(sessionId: Long): List<SessionExerciseLog>
}
