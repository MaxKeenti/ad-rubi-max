package com.example.bachewatch.data.repository.firestore

import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.Reporte

/**
 * Turn merged geohash sub-query results into the viewport's true contents:
 * dedupe (overlapping cells return the same doc), drop geohash false
 * positives (bounds over-cover by design, ADR-0002), drop soft-deleted.
 * Pure on purpose — task 13 unit-tests it without Firestore.
 */
internal fun filtrarViewport(reportes: List<Reporte>, bounds: GeoBounds): List<Reporte> =
    reportes
        .distinctBy { it.id }
        .filter { it.deletedAt == null && bounds.contains(it.lat, it.lng) }
