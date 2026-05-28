package com.example.mangos.data.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val MXN_FORMAT: DecimalFormat = DecimalFormat(
    "#,##0.00",
    DecimalFormatSymbols(Locale.US),
)

/** Formats Long centavos as "$1,234.56 MXN". Null → "—". */
fun Long?.centavosToMxnString(): String {
    if (this == null) return "—"
    val pesos = this / 100.0
    return "$" + MXN_FORMAT.format(pesos) + " MXN"
}

/**
 * Parses "1234.56" or "1,234.56" → 123456L. Empty/blank → null.
 * Invalid → throws IllegalArgumentException.
 */
fun String.parseMxnToCentavos(): Long? {
    if (this.isBlank()) return null
    val cleaned = this.trim().removePrefix("$").trim().replace(",", "")
    val parsed = cleaned.toBigDecimalOrNull()
        ?: throw IllegalArgumentException("Invalid money string: '$this'")
    return parsed.movePointRight(2).toLong()
}
