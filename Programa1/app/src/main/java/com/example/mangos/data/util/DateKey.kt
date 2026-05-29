package com.example.mangos.data.util

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val MX_ZONE: ZoneId = ZoneId.of("America/Mexico_City")

fun Timestamp.toDateKey(zone: ZoneId = MX_ZONE): String {
    val instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
    return instant.atZone(zone).toLocalDate().toString()
}

fun todayDateKey(zone: ZoneId = MX_ZONE): String = LocalDate.now(zone).toString()
