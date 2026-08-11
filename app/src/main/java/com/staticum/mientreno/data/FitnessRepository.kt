package com.staticum.mientreno.data

import kotlinx.coroutines.flow.Flow

class FitnessRepository(
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val sessionDao: SessionDao
) {
    fun observeExercises(): Flow<List<Exercise>> = exerciseDao.observeAll()

    suspend fun getExercise(exerciseId: Long): Exercise? = exerciseDao.getById(exerciseId)

    suspend fun saveExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)

    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)

    suspend fun seedExercisesIfEmpty() {
        if (exerciseDao.count() == 0) {
            exerciseDao.insertAll(DefaultExercises.all)
        }
    }

    fun observeRoutines(): Flow<List<RoutineTemplate>> = routineDao.observeAllRoutines()

    fun observeRoutine(routineId: Long): Flow<RoutineWithExercises?> =
        routineDao.observeRoutineWithExercises(routineId)

    suspend fun saveRoutine(routine: RoutineTemplate, exercises: List<RoutineExercise>): Long {
        val routineId = routineDao.insertRoutine(routine)
        val effectiveId = if (routine.id != 0L) routine.id else routineId
        val withRoutineId = exercises.mapIndexed { index, exercise ->
            exercise.copy(routineId = effectiveId, orderIndex = index)
        }
        routineDao.replaceRoutineExercises(effectiveId, withRoutineId)
        return effectiveId
    }

    suspend fun deleteRoutine(routine: RoutineTemplate) = routineDao.deleteRoutine(routine)

    fun observeSessions(): Flow<List<WorkoutSession>> = sessionDao.observeAllSessions()

    fun observeSession(sessionId: Long): Flow<SessionWithLogs?> =
        sessionDao.observeSessionWithLogs(sessionId)

    suspend fun saveSession(session: WorkoutSession, logs: List<SessionExerciseLog>): Long {
        val sessionId = sessionDao.insertSession(session)
        val withSessionId = logs.map { it.copy(sessionId = sessionId) }
        sessionDao.insertLogs(withSessionId)
        return sessionId
    }

    suspend fun deleteSession(session: WorkoutSession) = sessionDao.deleteSession(session)

    suspend fun getSessionsBetween(startMillis: Long, endMillis: Long): List<WorkoutSession> =
        sessionDao.getBetween(startMillis, endMillis)
}
