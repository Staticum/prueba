package com.staticum.mientreno.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineTemplateId: Long? = null,
    val routineName: String? = null,
    val startMillis: Long,
    val endMillis: Long? = null,
    val notes: String? = null
)

@Entity(tableName = "session_exercise_logs")
data class SessionExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val type: ExerciseType,
    val orderIndex: Int,
    val setNumber: Int = 1,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Int? = null,
    val completed: Boolean = true
)

data class SessionWithLogs(
    @Embedded val session: WorkoutSession,
    @Relation(
        entity = SessionExerciseLog::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val logs: List<SessionExerciseLog>
)
