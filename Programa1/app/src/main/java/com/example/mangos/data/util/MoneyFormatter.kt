package com.example.mangos.data.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val MX_LOCALE: Locale = Locale.forLanguageTag("es-MX")

fun Long?.centavosToMxnString(): String {
    if (this == null) return "-"
    val pesos = BigDecimal(this).movePointLeft(2)
    val formatted = NumberFormat.getCurrencyInstance(MX_LOCALE).format(pesos)
    return "$formatted MXN"
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
        throw IllegalArgumentException("Precio inválido", e)
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Precio inválido", e)
    }
}
