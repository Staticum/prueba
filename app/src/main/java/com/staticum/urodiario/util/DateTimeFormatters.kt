package com.staticum.urodiario.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateTimeFormatters {
    private val zone = ZoneId.systemDefault()

    val dateLabel: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale("es", "ES"))

    val dateShort: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))

    val timeLabel: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale("es", "ES"))

    fun formatDateTime(millis: Long): String {
        val instant = Instant.ofEpochMilli(millis).atZone(zone)
        return "${instant.format(dateShort)} · ${instant.format(timeLabel)}"
    }

    fun formatDuration(seconds: Int?): String {
        if (seconds == null) return "—"
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
