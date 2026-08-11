package com.staticum.mientreno.ui.progress

data class DailySummary(
    val dayStartMillis: Long,
    val label: String,
    val sessionCount: Int,
    val totalMinutes: Int
)
