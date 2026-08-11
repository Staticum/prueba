package com.staticum.mientreno.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticum.mientreno.data.FitnessRepository
import com.staticum.mientreno.data.WorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ProgressViewModel(repository: FitnessRepository) : ViewModel() {

    private val sessions: StateFlow<List<WorkoutSession>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummaries: StateFlow<List<DailySummary>> = sessions
        .map { buildDailySummaries(it, days = 14) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionsLast7Days: StateFlow<Int> = dailySummaries
        .map { it.takeLast(7).sumOf { day -> day.sessionCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMinutesLast7Days: StateFlow<Int> = dailySummaries
        .map { it.takeLast(7).sumOf { day -> day.totalMinutes } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun buildDailySummaries(sessions: List<WorkoutSession>, days: Int): List<DailySummary> {
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val labelFormatter = DateTimeFormatter.ofPattern("dd/MM")
        val buckets = (0 until days).map { offset -> today.minusDays(offset.toLong()) }.reversed()

        return buckets.map { day ->
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val daySessions = sessions.filter { it.startMillis in dayStart until dayEnd }
            val totalMinutes = daySessions.sumOf { session ->
                val end = session.endMillis ?: session.startMillis
                ((end - session.startMillis) / 60000).toInt()
            }
            DailySummary(
                dayStartMillis = dayStart,
                label = day.format(labelFormatter),
                sessionCount = daySessions.size,
                totalMinutes = totalMinutes
            )
        }
    }
}
