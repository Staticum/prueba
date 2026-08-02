package com.staticum.urodiario.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.urodiario.data.MicturitionRepository
import com.staticum.urodiario.data.RecordWithPhotos
import com.staticum.urodiario.util.DateTimeFormatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

class ReportsViewModel(private val repository: MicturitionRepository) : ViewModel() {

    val recentRecords: StateFlow<List<RecordWithPhotos>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummaries: StateFlow<List<DailySummary>> = recentRecords
        .map { records -> buildDailySummaries(records, days = 14) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val averageVolumeMl: StateFlow<Int> = recentRecords
        .map { records ->
            val volumes = records.mapNotNull { it.record.volumeMl }
            if (volumes.isEmpty()) 0 else volumes.sum() / volumes.size
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageFrequencyPerDay: StateFlow<Double> = dailySummaries
        .map { summaries ->
            if (summaries.isEmpty()) 0.0
            else summaries.sumOf { it.count }.toDouble() / summaries.size
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    suspend fun getAllRecordsForExport() = repository.getAllForExport()

    private fun buildDailySummaries(records: List<RecordWithPhotos>, days: Int): List<DailySummary> {
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val buckets = (0 until days).map { offset -> today.minusDays(offset.toLong()) }.reversed()

        return buckets.map { day ->
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val dayRecords = records.filter { it.record.dateTimeMillis in dayStart until dayEnd }
            DailySummary(
                dayStartMillis = dayStart,
                label = day.format(DateTimeFormatters.dateShort),
                count = dayRecords.size,
                totalVolumeMl = dayRecords.mapNotNull { it.record.volumeMl }.sum(),
                bloodOrLeakageCount = dayRecords.count { it.record.hasBlood || it.record.hasLeakage }
            )
        }
    }
}
