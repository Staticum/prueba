package com.staticum.diariocalorico.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeFormatters {
    val dateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val date: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy")

    fun formatDateTime(instant: Instant): String =
        dateTime.withZone(ZoneId.systemDefault()).format(instant)

    fun formatDate(instant: Instant): String =
        date.withZone(ZoneId.systemDefault()).format(instant)

    fun formatDate(localDate: LocalDate): String = date.format(localDate)

    fun formatLongDate(localDate: LocalDate): String =
        longDate.format(localDate).replaceFirstChar { it.uppercase() }
}
