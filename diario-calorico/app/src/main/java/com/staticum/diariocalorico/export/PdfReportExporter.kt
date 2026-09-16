package com.staticum.diariocalorico.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.staticum.diariocalorico.data.MealEntry
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.io.File
import java.io.FileOutputStream

object PdfReportExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val LINE_HEIGHT = 16f

    fun export(context: Context, meals: List<MealEntry>): android.net.Uri {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10f }
        val smallPaint = Paint().apply { textSize = 8f; color = 0xFF666666.toInt() }

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

        val totalCalories = meals.sumOf { it.calories }
        canvas.drawText("Total registros: ${meals.size}  ·  Total kcal: $totalCalories", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT * 2

        meals.forEach { meal ->
            if (y > PAGE_HEIGHT - MARGIN * 2) newPage()
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
}
