package com.example.mangos.data.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val MXN_FORMAT: DecimalFormat = DecimalFormat(
    "#,##0.00",
    DecimalFormatSymbols(Locale.US),
)

fun Long?.centavosToMxnString(): String {
    if (this == null) return "-"
    val pesos = BigDecimal(this).movePointLeft(2)
    return "$" + MXN_FORMAT.format(pesos) + " MXN"
}

fun String.parseMxnToCentavos(): Long? {
    val normalized = trim()
        .replace("$", "")
        .replace("MXN", "", ignoreCase = true)
        .replace(",", "")
        .trim()

    if (normalized.isBlank()) return null

    return try {
        BigDecimal(normalized)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
    } catch (e: ArithmeticException) {
        throw IllegalArgumentException("Precio invalido", e)
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Precio invalido", e)
    }
}
