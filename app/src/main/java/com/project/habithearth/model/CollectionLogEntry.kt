package com.project.habithearth.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CollectionLogEntry(
    val timestamp: LocalDateTime,
    val amount: Int,
) {
    fun getFormattedTime(): String =
        timestamp.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
}

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun LocalDateTime.toEpochMs(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
