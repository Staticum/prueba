package com.staticum.urodiario.export

import android.content.Context
import androidx.core.content.FileProvider
import com.staticum.urodiario.data.MicturitionRecord
import com.staticum.urodiario.util.DateTimeFormatters
import java.io.File
import java.io.FileWriter

object CsvExporter {

    fun export(context: Context, records: List<MicturitionRecord>): android.net.Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "urodiario_export_${System.currentTimeMillis()}.csv")

        FileWriter(file).use { writer ->
            writer.append("Fecha,Hora,Volumen (ml),Duracion (s),Color,Olor,Urgencia (0-4),Dolor (0-10),Sangre,Escape,Nocturno,Ingesta liquidos (ml),Notas\n")
            records.forEach { record ->
                val zoned = java.time.Instant.ofEpochMilli(record.dateTimeMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                writer.append(zoned.format(DateTimeFormatters.dateShort)).append(',')
                writer.append(zoned.format(DateTimeFormatters.timeLabel)).append(',')
                writer.append(record.volumeMl?.toString() ?: "").append(',')
                writer.append(record.durationSeconds?.toString() ?: "").append(',')
                writer.append(record.color.label.escapeCsv()).append(',')
                writer.append(record.odor.label.escapeCsv()).append(',')
                writer.append(record.urgency.toString()).append(',')
                writer.append(record.painLevel.toString()).append(',')
                writer.append(if (record.hasBlood) "Si" else "No").append(',')
                writer.append(if (record.hasLeakage) "Si" else "No").append(',')
                writer.append(if (record.isNocturnal) "Si" else "No").append(',')
                writer.append(record.fluidIntakeMl?.toString() ?: "").append(',')
                writer.append((record.notes ?: "").escapeCsv()).append('\n')
            }
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun String.escapeCsv(): String =
        if (contains(",") || contains("\"") || contains("\n")) {
            "\"" + replace("\"", "\"\"") + "\""
        } else this
}
