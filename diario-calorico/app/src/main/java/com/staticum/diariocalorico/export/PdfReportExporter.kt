package com.staticum.diariocalorico.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.staticum.diariocalorico.ui.reports.ReportsUiState
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.io.File
import java.io.FileOutputStream

object PdfReportExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val LINE_HEIGHT = 16f

    fun export(context: Context, state: ReportsUiState): android.net.Uri {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val sectionPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10f }
        val smallPaint = Paint().apply { textSize = 8f; color = Color.rgb(0x66, 0x66, 0x66) }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas: Canvas = page.canvas
        var y = MARGIN

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        canvas.drawText("Reporte Diario Calórico", MARGIN, y, titlePaint)
        y += LINE_HEIGHT * 2

        canvas.drawText(
            "${DateTimeFormatters.formatDate(state.startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())} - " +
                "${DateTimeFormatters.formatDate(state.endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())}",
            MARGIN, y, bodyPaint
        )
        y += LINE_HEIGHT * 2

        // KPIs
        canvas.drawText("Resumen del período", MARGIN, y, sectionPaint)
        y += LINE_HEIGHT * 1.5f
        val kpis = state.kpis
        canvas.drawText("Promedio diario: ${kpis.avgCalories} kcal", MARGIN, y, bodyPaint); y += LINE_HEIGHT
        canvas.drawText("Días dentro de meta: ${kpis.daysOnTrack} / ${kpis.daysOnTrack + kpis.daysOffTrack}", MARGIN, y, bodyPaint); y += LINE_HEIGHT
        canvas.drawText("Macro dominante: ${kpis.dominantMacroLabel}", MARGIN, y, bodyPaint); y += LINE_HEIGHT
        canvas.drawText("Racha actual dentro de meta: ${kpis.streakDays} días", MARGIN, y, bodyPaint); y += LINE_HEIGHT
        kpis.avgVsPreviousPeriod?.let {
            canvas.drawText("Vs. período anterior: ${if (it >= 0) "+" else ""}$it kcal/día promedio", MARGIN, y, bodyPaint)
            y += LINE_HEIGHT
        }
        y += LINE_HEIGHT

        // Gráfico de calorías por día dibujado directo en el PDF
        canvas.drawText("Calorías consumidas por día", MARGIN, y, sectionPaint)
        y += LINE_HEIGHT
        val chartHeight = 140f
        val chartWidth = PAGE_WIDTH - MARGIN * 2
        val chartRect = RectF(MARGIN, y, MARGIN + chartWidth, y + chartHeight)
        drawBarChart(canvas, chartRect, state.dailyCalories.map { it.value }, state.goals.calories)
        y += chartHeight + LINE_HEIGHT * 2

        // Detalle de comidas
        canvas.drawText("Detalle de comidas", MARGIN, y, sectionPaint)
        y += LINE_HEIGHT * 1.5f

        val meals = state.filteredMeals.ifEmpty { state.mealsInRange }
        meals.forEach { meal ->
            if (y > PAGE_HEIGHT - MARGIN * 3) newPage()
            canvas.drawText(
                "${DateTimeFormatters.formatDateTime(meal.consumedAt)} - ${meal.mealType.label} - ${meal.calories} kcal",
                MARGIN, y, bodyPaint
            )
            y += LINE_HEIGHT
            canvas.drawText(
                "  P:${meal.proteinGrams}g C:${meal.carbsGrams}g G:${meal.fatGrams}g - ${meal.detectedFoods}",
                MARGIN, y, smallPaint
            )
            y += LINE_HEIGHT * 1.5f
        }

        document.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "diario_calorico_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun drawBarChart(canvas: Canvas, rect: RectF, values: List<Int>, goal: Int) {
        val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(goal).coerceAtLeast(1)
        val barPaint = Paint().apply { color = Color.rgb(0x2E, 0x7D, 0x32) }
        val goalPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        val gridPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        repeat(5) { i ->
            val gy = rect.top + rect.height() * i / 4
            canvas.drawLine(rect.left, gy, rect.right, gy, gridPaint)
        }

        if (values.isNotEmpty()) {
            val slotWidth = rect.width() / values.size
            val barWidth = slotWidth * 0.6f
            values.forEachIndexed { index, value ->
                val barHeight = rect.height() * (value.toFloat() / maxValue)
                val x = rect.left + index * slotWidth + slotWidth * 0.2f
                canvas.drawRect(x, rect.bottom - barHeight, x + barWidth, rect.bottom, barPaint)
            }
        }

        val goalY = rect.bottom - rect.height() * (goal.toFloat() / maxValue)
        canvas.drawLine(rect.left, goalY, rect.right, goalY, goalPaint)
    }
}
