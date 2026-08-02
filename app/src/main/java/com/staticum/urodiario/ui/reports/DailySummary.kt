package com.staticum.urodiario.ui.reports

data class DailySummary(
    val dayStartMillis: Long,
    val label: String,
    val count: Int,
    val totalVolumeMl: Int,
    val bloodOrLeakageCount: Int
)
