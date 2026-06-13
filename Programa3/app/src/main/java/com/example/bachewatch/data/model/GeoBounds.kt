package com.example.bachewatch.data.model

/**
 * Our own viewport type so the repository contract stays Maps-SDK-free
 * and unit-testable (ADR-0002). UI converts LatLngBounds ↔ GeoBounds.
 * Antimeridian crossing is deliberately unhandled — CDMX-scoped app
 * (documented assumption, asserted in tests).
 */
data class GeoBounds(
    val swLat: Double,
    val swLng: Double,
    val neLat: Double,
    val neLng: Double,
) {
    fun contains(lat: Double, lng: Double): Boolean =
        lat in swLat..neLat && lng in swLng..neLng

    companion object {
        /** Default home viewport: greater CDMX. */
        val CDMX = GeoBounds(swLat = 19.20, swLng = -99.30, neLat = 19.55, neLng = -98.95)
    }
}
