package com.staticum.mientreno.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    val summaries by viewModel.dailySummaries.collectAsState()
    val sessionsLast7 by viewModel.sessionsLast7Days.collectAsState()
    val minutesLast7 by viewModel.totalMinutesLast7Days.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SummaryCard("Sesiones (7 días)", "$sessionsLast7", Modifier.fillMaxWidth(0.48f))
                SummaryCard("Minutos (7 días)", "$minutesLast7", Modifier.fillMaxWidth(0.48f))
            }

            Text("Sesiones últimos 14 días", style = MaterialTheme.typography.labelMedium)
            SessionsPerDayBarChart(summaries = summaries)
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
