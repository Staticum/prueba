package com.staticum.mientreno.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

fun Long.toFormattedDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(dateFormatter)

fun Long.toFormattedDateTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

fun Int.toFormattedDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return if (minutes > 0) "${minutes} min ${seconds} s" else "${seconds} s"
}
