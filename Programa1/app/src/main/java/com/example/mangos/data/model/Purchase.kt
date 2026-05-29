package com.example.mangos.data.model

import com.google.firebase.Timestamp

data class Purchase(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val supplierNoteFreeform: String? = null,
    val quantityTons: Double,
    val pricePerTonCentavos: Long? = null,
    val date: Timestamp,
    val dateKey: String,
    val createdBy: String,
    val createdByName: String,
    val enteredAt: Timestamp,
    val serverWrittenAt: Timestamp,
    val deletedAt: Timestamp? = null,
    val deletedBy: String? = null,
)
