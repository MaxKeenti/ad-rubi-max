package com.example.mangos.data.util

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MX_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/** Returns "YYYY-MM-DD" in Mexico City local time (by default). */
fun Timestamp.toDateKey(zone: ZoneId = MX_ZONE): String {
    val instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
    return instant.atZone(zone).toLocalDate().format(ISO_DATE)
}

/** Returns today's dateKey. Convenience for queries. */
fun todayDateKey(zone: ZoneId = MX_ZONE): String =
    LocalDate.now(zone).format(ISO_DATE)
