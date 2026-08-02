package com.staticum.urodiario.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.staticum.urodiario.data.MicturitionRecord
import com.staticum.urodiario.util.DateTimeFormatters
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId

object PdfReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val LINE_HEIGHT = 16f

    fun export(context: Context, records: List<MicturitionRecord>): android.net.Uri {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10f; typeface = Typeface.DEFAULT }
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

        canvas.drawText("Diario Miccional — Reporte", MARGIN, y, titlePaint)
        y += 24f
        canvas.drawText(
            "Generado: ${DateTimeFormatters.formatDateTime(System.currentTimeMillis())} · ${records.size} registros",
            MARGIN, y, smallPaint
        )
        y += 20f

        val columns = listOf("Fecha/Hora", "Vol.(ml)", "Dur.(s)", "Color", "Olor", "Urg.", "Dolor", "Sangre/Escape")
        val columnX = floatArrayOf(MARGIN, 150f, 200f, 240f, 320f, 400f, 430f, 465f)

        fun drawHeaderRow() {
            columns.forEachIndexed { i, col -> canvas.drawText(col, columnX[i], y, headerPaint) }
            y += LINE_HEIGHT
            canvas.drawLine(MARGIN, y - 4f, PAGE_WIDTH - MARGIN, y - 4f, smallPaint)
        }

        drawHeaderRow()

        records.forEach { record ->
            if (y > PAGE_HEIGHT - MARGIN - LINE_HEIGHT) {
                newPage()
                drawHeaderRow()
            }
            val zoned = Instant.ofEpochMilli(record.dateTimeMillis).atZone(ZoneId.systemDefault())
            val values = listOf(
                "${zoned.format(DateTimeFormatters.dateShort)} ${zoned.format(DateTimeFormatters.timeLabel)}",
                record.volumeMl?.toString() ?: "-",
                record.durationSeconds?.toString() ?: "-",
                record.color.label.take(10),
                record.odor.label.take(10),
                record.urgency.toString(),
                record.painLevel.toString(),
                "${if (record.hasBlood) "S" else "N"}/${if (record.hasLeakage) "S" else "N"}"
            )
            values.forEachIndexed { i, v -> canvas.drawText(v, columnX[i], y, bodyPaint) }
            y += LINE_HEIGHT

            if (!record.notes.isNullOrBlank()) {
                if (y > PAGE_HEIGHT - MARGIN - LINE_HEIGHT) {
                    newPage()
                    drawHeaderRow()
                }
                canvas.drawText("Nota: ${record.notes.take(90)}", MARGIN + 8f, y, smallPaint)
                y += LINE_HEIGHT
            }
        }

        document.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "urodiario_reporte_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
