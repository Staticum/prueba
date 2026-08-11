package com.staticum.mientreno

import android.app.Application
import com.staticum.mientreno.data.AppDatabase
import com.staticum.mientreno.data.FitnessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MiEntrenoApplication : Application() {

    lateinit var repository: FitnessRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = FitnessRepository(
            exerciseDao = database.exerciseDao(),
            routineDao = database.routineDao(),
            sessionDao = database.sessionDao()
        )
        applicationScope.launch {
            repository.seedExercisesIfEmpty()
        }
    }
}
