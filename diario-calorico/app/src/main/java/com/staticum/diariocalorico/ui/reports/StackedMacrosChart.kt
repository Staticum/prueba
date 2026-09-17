package com.staticum.diariocalorico.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE d", Locale("es", "ES"))
private val proteinColor = Color(0xFF2E7D32)
private val carbsColor = Color(0xFFFFA000)
private val fatColor = Color(0xFFD32F2F)

@Composable
fun StackedMacrosChart(points: List<DayMacros>, modifier: Modifier = Modifier) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxTotal = points.maxOfOrNull { it.proteinGrams + it.carbsGrams + it.fatGrams }?.coerceAtLeast(1.0) ?: 1.0

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val chartHeight = size.height - 24.dp.toPx()
            val barWidth = size.width / (points.size.coerceAtLeast(1) * 1.5f)

            points.forEachIndexed { index, day ->
                val x = index * barWidth * 1.5f + barWidth * 0.25f
                var yCursor = chartHeight
                listOf(day.proteinGrams to proteinColor, day.carbsGrams to carbsColor, day.fatGrams to fatColor)
                    .forEach { (value, color) ->
                        val segmentHeight = (chartHeight * (value / maxTotal)).toFloat()
                        drawRect(color = color, topLeft = Offset(x, yCursor - segmentHeight), size = Size(barWidth, segmentHeight))
                        yCursor -= segmentHeight
                    }
            }

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            points.forEachIndexed { index, day ->
                val x = index * barWidth * 1.5f + barWidth * 0.25f + barWidth / 2
                drawContext.canvas.nativeCanvas.drawText(day.date.format(dayLabelFormatter), x, size.height - 4.dp.toPx(), labelPaint)
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            LegendDot(proteinColor, "Proteína")
            LegendDot(carbsColor, "Carbohidratos", start = 16.dp)
            LegendDot(fatColor, "Grasa", start = 16.dp)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, start: androidx.compose.ui.unit.Dp = 0.dp) {
    Row(modifier = Modifier.padding(start = start)) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp))
        )
        Text(" $label", style = MaterialTheme.typography.bodySmall)
    }
}
