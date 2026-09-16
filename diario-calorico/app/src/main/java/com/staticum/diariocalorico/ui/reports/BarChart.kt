package com.staticum.diariocalorico.ui.reports

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
fun CaloriesBarChart(values: List<Int>, goal: Int, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.error
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(goal).coerceAtLeast(1)

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val barWidth = size.width / (values.size.coerceAtLeast(1) * 1.5f)
        values.forEachIndexed { index, value ->
            val barHeight = size.height * (value.toFloat() / maxValue)
            val x = index * barWidth * 1.5f
            drawRect(
                color = barColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
        val goalY = size.height * (1 - goal.toFloat() / maxValue)
        drawLine(goalColor, Offset(0f, goalY), Offset(size.width, goalY), strokeWidth = 3f)
    }
}
