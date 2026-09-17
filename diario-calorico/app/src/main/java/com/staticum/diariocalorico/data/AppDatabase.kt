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
        DailyExpenditure::class, WeightEntry::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun trackingDao(): TrackingDao

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

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diario_calorico.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build().also { instance = it }
            }
    }
}
