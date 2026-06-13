package com.example.bachewatch.data.model

/** Single-shot GPS fix captured at the pothole (grilling Q3/Q5). */
data class LocationFix(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Double,
) {
    companion object {
        /** Soft gate threshold: worse than this is flagged, never rejected. */
        const val UMBRAL_PRECISION_M = 25.0
    }
}
