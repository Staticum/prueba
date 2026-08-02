package com.staticum.urodiario

import android.app.Application
import com.staticum.urodiario.data.AppDatabase
import com.staticum.urodiario.data.MicturitionRepository

class UroDiarioApplication : Application() {

    lateinit var repository: MicturitionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = MicturitionRepository(database.micturitionDao())
    }
}
