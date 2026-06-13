package com.example.bachewatch.data.location

import com.example.bachewatch.data.model.LocationFix

interface LocationProvider {
    /**
     * Single-shot high-accuracy fix at the moment of capture (Q5).
     * Failure is an expected outcome (no permission, timeout, GPS off),
     * hence Result rather than throwing.
     */
    suspend fun fixActual(): Result<LocationFix>
}

/**
 * Expected fix failures (Q5). The UI distinguishes [SinPermiso] (drive the
 * rationale path) from [FixTimeout]/[FixNoDisponible] (offer Reintentar).
 */
sealed class LocationError(message: String) : Exception(message) {
    object SinPermiso : LocationError("Se necesita ubicación precisa para reportar")
    object FixTimeout : LocationError("No se pudo obtener la ubicación; reintenta")
    object FixNoDisponible : LocationError("Ubicación no disponible; reintenta")
}
