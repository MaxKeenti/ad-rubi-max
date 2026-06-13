package com.example.bachewatch.data.util

import android.text.format.DateUtils
import com.google.firebase.Timestamp

/** "hace 2 horas" — device locale gives Spanish for free. */
fun tiempoRelativo(ts: Timestamp?): String =
    ts?.let {
        DateUtils.getRelativeTimeSpanString(
            it.toDate().time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    } ?: "—"
