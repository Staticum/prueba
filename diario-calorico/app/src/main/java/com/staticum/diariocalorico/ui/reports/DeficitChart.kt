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
private val consumedColor = Color(0xFF2E7D32)
private val expenditureColor = Color(0xFFFFA000)
private val weightColor = Color(0xFF1565C0)

@Composable
fun DeficitChart(
    consumed: List<DayPoint>,
    expenditure: List<DayPoint>,
    weight: List<DayWeight>,
    modifier: Modifier = Modifier
) {
    val maxCalories = (consumed + expenditure).maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    val weightValues = weight.mapNotNull { it.weightKg }
    val minWeight = weightValues.minOrNull() ?: 0.0
    val maxWeight = (weightValues.maxOrNull() ?: 1.0).coerceAtLeast(minWeight + 0.1)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val chartHeight = size.height - 24.dp.toPx()
            val groupWidth = size.width / consumed.size.coerceAtLeast(1)
            val barWidth = groupWidth * 0.35f

            consumed.forEachIndexed { index, point ->
                val barHeight = chartHeight * (point.value.toFloat() / maxCalories)
                val x = index * groupWidth + groupWidth * 0.1f
                drawRect(consumedColor, topLeft = Offset(x, chartHeight - barHeight), size = Size(barWidth, barHeight))
            }
            expenditure.forEachIndexed { index, point ->
                val barHeight = chartHeight * (point.value.toFloat() / maxCalories)
                val x = index * groupWidth + groupWidth * 0.1f + barWidth
                drawRect(expenditureColor, topLeft = Offset(x, chartHeight - barHeight), size = Size(barWidth, barHeight))
            }

            if (weightValues.size >= 2) {
                val weightPoints = weight.mapIndexedNotNull { index, dw ->
                    dw.weightKg?.let { w ->
                        val x = index * groupWidth + groupWidth / 2
                        val ratio = (w - minWeight) / (maxWeight - minWeight)
                        val y = chartHeight - (chartHeight * ratio).toFloat()
                        Offset(x, y)
                    }
                }
                for (i in 0 until weightPoints.size - 1) {
                    drawLine(weightColor, weightPoints[i], weightPoints[i + 1], strokeWidth = 4f)
                }
                weightPoints.forEach { drawCircle(weightColor, radius = 5f, center = it) }
            }

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            consumed.forEachIndexed { index, point ->
                val x = index * groupWidth + groupWidth / 2
                drawContext.canvas.nativeCanvas.drawText(point.date.format(dayLabelFormatter), x, size.height - 4.dp.toPx(), labelPaint)
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            LegendDot(consumedColor, "Consumidas")
            LegendDot(expenditureColor, "Gastadas (Polar)", start = 16.dp)
            if (weightValues.isNotEmpty()) LegendDot(weightColor, "Peso", start = 16.dp)
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
