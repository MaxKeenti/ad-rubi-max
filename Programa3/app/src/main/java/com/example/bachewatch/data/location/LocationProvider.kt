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
