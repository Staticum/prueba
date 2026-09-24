package com.staticum.diariocalorico.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MealEntry::class, LabelPhoto::class, FoodPhoto::class,
        DailyExpenditure::class, WeightEntry::class, GeminiLogEntry::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun trackingDao(): TrackingDao
    abstract fun geminiLogDao(): GeminiLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Agrega las tablas de gasto calórico diario y peso, sin tocar las comidas ya
         * guardadas. Nunca usar fallbackToDestructiveMigration en esta app: los registros
         * del usuario no son descartables.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_expenditure` (" +
                        "`date` INTEGER NOT NULL, `caloriesBurned` INTEGER NOT NULL, PRIMARY KEY(`date`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weight_entries` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`recordedAt` INTEGER NOT NULL, `weightKg` REAL NOT NULL)"
                )
            }
        }

        /**
         * Agrega el marcador de "análisis pendiente" a las comidas (para el reintento
         * automático en segundo plano cuando Gemini no responde a tiempo) y la tabla de log
         * de fallos de Gemini, para poder diagnosticar problemas de conectividad recurrentes.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN analysisPending INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `gemini_log` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `context` TEXT NOT NULL, " +
                        "`model` TEXT, `message` TEXT NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diario_calorico.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}
