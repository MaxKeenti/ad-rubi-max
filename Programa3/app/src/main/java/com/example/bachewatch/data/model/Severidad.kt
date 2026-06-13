package com.example.bachewatch.data.model

/**
 * Optional, one-tap, no default (grilling Q6). [peso] feeds the
 * severity-weighted heatmap (Q10) — the single source for weights.
 */
enum class Severidad(val valor: String, val peso: Double) {
    LEVE("leve", 1.0),
    MODERADO("moderado", 2.0),
    SEVERO("severo", 3.0);

    companion object {
        /** Heatmap weight for reportes without severidad (CONTEXT.md). */
        const val PESO_SIN_SEVERIDAD = 1.5

        fun fromValor(valor: String?): Severidad? =
            entries.firstOrNull { it.valor == valor }
    }
}
