package com.staticum.diariocalorico.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE d", Locale("es", "ES"))

@Composable
fun CaloriesBarChart(points: List<DayPoint>, goal: Int, modifier: Modifier = Modifier) {
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = ((points.maxOfOrNull { it.value } ?: 0).coerceAtLeast(goal)).coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isEmpty()) return@detectTapGestures
                        val slotWidth = size.width / points.size
                        val index = (offset.x / slotWidth).toInt().coerceIn(0, points.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
        ) {
            val chartHeight = size.height - 24.dp.toPx()
            val gridLines = 4
            repeat(gridLines + 1) { i ->
                val y = chartHeight * i / gridLines
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            val barWidth = size.width / (points.size.coerceAtLeast(1) * 1.5f)
            points.forEachIndexed { index, point ->
                val barHeight = chartHeight * (point.value.toFloat() / maxValue)
                val x = index * barWidth * 1.5f + barWidth * 0.25f
                val color = if (selectedIndex == index) barColor.copy(alpha = 0.7f) else barColor
                drawRect(
                    color = color,
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }

            val goalY = chartHeight * (1 - goal.toFloat() / maxValue)
            drawLine(
                goalColor,
                Offset(0f, goalY),
                Offset(size.width, goalY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
            drawContext.canvas.nativeCanvas.drawText(
                "Meta: $goal kcal",
                4f,
                (goalY - 6f).coerceAtLeast(12f),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    textSize = 11.sp.toPx()
                }
            )

            val labelPaint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            points.forEachIndexed { index, point ->
                val x = index * barWidth * 1.5f + barWidth * 0.25f + barWidth / 2
                drawContext.canvas.nativeCanvas.drawText(
                    point.date.format(dayLabelFormatter),
                    x,
                    size.height - 4.dp.toPx(),
                    labelPaint
                )
            }
        }

        selectedIndex?.let { index ->
            val point = points.getOrNull(index)
            if (point != null) {
                Text(
                    "${DateTimeFormatters.formatDate(point.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())}: ${point.value} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )
}
