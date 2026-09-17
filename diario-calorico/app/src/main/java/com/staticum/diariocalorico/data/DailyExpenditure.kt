package com.staticum.diariocalorico.data

import androidx.room.Entity
import java.time.LocalDate

@Entity(tableName = "daily_expenditure", primaryKeys = ["date"])
data class DailyExpenditure(
    val date: LocalDate,
    val caloriesBurned: Int
)
