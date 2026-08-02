package com.staticum.urodiario.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun DailyCountBarChart(summaries: List<DailySummary>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val alertColor = MaterialTheme.colorScheme.error
    val maxCount = (summaries.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)

    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        if (summaries.isEmpty()) return@Canvas
        val barSpacing = size.width / summaries.size
        val barWidth = barSpacing * 0.6f

        summaries.forEachIndexed { index, summary ->
            val barHeight = (summary.count.toFloat() / maxCount) * (size.height - 10.dp.toPx())
            val left = index * barSpacing + (barSpacing - barWidth) / 2f
            val top = size.height - barHeight
            drawRect(
                color = if (summary.bloodOrLeakageCount > 0) alertColor else barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight)
            )
        }
        drawLine(
            color = barColor.copy(alpha = 0.3f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }
}
