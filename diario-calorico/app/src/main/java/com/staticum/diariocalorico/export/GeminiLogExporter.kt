package com.staticum.diariocalorico.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.staticum.diariocalorico.data.GeminiLogEntry
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.io.File

object GeminiLogExporter {
    fun export(context: Context, entries: List<GeminiLogEntry>): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "gemini_log_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { writer ->
            writer.appendLine("Fecha,Contexto,Modelo,Mensaje")
            entries.forEach { entry ->
                writer.appendLine(
                    listOf(
                        DateTimeFormatters.formatDateTime(entry.timestamp),
                        "\"${entry.context.replace("\"", "'")}\"",
                        entry.model ?: "",
                        "\"${entry.message.replace("\"", "'").replace("\n", " ")}\""
                    ).joinToString(",")
                )
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
