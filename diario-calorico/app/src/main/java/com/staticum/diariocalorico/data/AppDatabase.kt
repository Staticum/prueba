package com.staticum.diariocalorico.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MealEntry::class, LabelPhoto::class, FoodPhoto::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diario_calorico.db"
                )
                    // App en etapa de pruebas: se prioriza avanzar rápido sobre preservar
                    // datos entre versiones del esquema.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
