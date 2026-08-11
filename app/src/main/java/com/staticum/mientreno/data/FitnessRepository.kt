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

    suspend fun syncDefaultExercises() {
        val existingNames = exerciseDao.getAllNames().toSet()
        val missing = DefaultExercises.all.filter { it.name !in existingNames }
        if (missing.isNotEmpty()) {
            exerciseDao.insertAll(missing)
        }
    }

    suspend fun seedDeskBikeRoutineIfMissing() {
        if (routineDao.countByName(DeskBikeRoutine.NAME) > 0) return
        val bikeExercise = exerciseDao.getByName(DeskBikeRoutine.EXERCISE_NAME) ?: return
        val (routine, blocks) = DeskBikeRoutine.build(bikeExercise)
        saveRoutine(routine, blocks)
    }

    fun observeRoutines(): Flow<List<RoutineTemplate>> = routineDao.observeAllRoutines()

    fun observeRoutine(routineId: Long): Flow<RoutineWithBlocks?> =
        routineDao.observeRoutineWithBlocks(routineId)

    suspend fun saveRoutine(routine: RoutineTemplate, blocks: List<Pair<RoutineBlock, List<RoutineExercise>>>): Long {
        val routineId = routineDao.insertRoutine(routine)
        val effectiveId = if (routine.id != 0L) routine.id else routineId
        val withRoutineId = blocks.mapIndexed { index, (block, exercises) ->
            block.copy(routineId = effectiveId, orderIndex = index) to
                exercises.mapIndexed { exIndex, exercise -> exercise.copy(orderIndex = exIndex) }
        }
        routineDao.replaceRoutineBlocks(effectiveId, withRoutineId)
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
