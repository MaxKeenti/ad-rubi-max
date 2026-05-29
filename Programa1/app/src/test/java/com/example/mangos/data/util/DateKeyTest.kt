package com.example.mangos.data.util

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DateKeyTest {

    private fun tsAt(iso: String): Timestamp {
        val instant = OffsetDateTime.parse(iso).toInstant()
        return Timestamp(instant.epochSecond, instant.nano)
    }

    @Test
    fun `late-evening Mexico City instant stays on local day`() {
        // 23:30 local on 2026-05-26 (UTC-6, no DST)
        val ts = tsAt("2026-05-26T23:30:00-06:00")
        assertEquals("2026-05-26", ts.toDateKey())
    }

    @Test
    fun `next local day crosses correctly`() {
        // Same instant family, one hour later → already 2026-05-27 local
        val ts = tsAt("2026-05-27T00:30:00-06:00")
        assertEquals("2026-05-27", ts.toDateKey())
    }

    @Test
    fun `default zone is Mexico City, not UTC`() {
        // 23:30 local in MX = 05:30 UTC next day. UTC would say 2026-05-27.
        val ts = tsAt("2026-05-26T23:30:00-06:00")
        val mxKey = ts.toDateKey()
        val utcKey = ts.toDateKey(ZoneId.of("UTC"))
        assertEquals("2026-05-26", mxKey)
        assertEquals("2026-05-27", utcKey)
        assertNotEquals(mxKey, utcKey)
    }

    @Test
    fun `todayDateKey returns ISO date in Mexico City zone`() {
        val expected = LocalDate.now(ZoneId.of("America/Mexico_City")).toString()
        assertEquals(expected, todayDateKey())
    }
}
