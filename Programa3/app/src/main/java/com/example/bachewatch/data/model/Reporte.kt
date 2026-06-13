package com.example.bachewatch.data.model

import com.google.firebase.Timestamp

/**
 * An observation, never an incident — immutable payload, soft-delete
 * only, duplicates welcome (CONTEXT.md). Schema frozen 2026-06-11
 * (implementation_plan.md §2). Timestamp is the one Firebase type
 * models may expose (P1 precedent).
 */
data class Reporte(
    val id: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geohash: String = "",
    val accuracyMeters: Double = 0.0,
    val severidad: Severidad? = null,
    val descripcion: String? = null,
    val fotoPath: String = "",
    val fotoUrl: String = "",
    val createdBy: String = "",
    val confirmCount: Long = 0,
    val serverWrittenAt: Timestamp? = null,
    val deletedAt: Timestamp? = null,
    val deletedBy: String? = null,
)
