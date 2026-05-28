package com.example.mangos.data.model

import com.google.firebase.Timestamp

data class Purchase(
    val id: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val supplierNoteFreeform: String? = null,
    val quantityTons: Double = 0.0,
    val pricePerTonCentavos: Long? = null,
    val date: Timestamp? = null,
    val dateKey: String = "",
    val createdBy: String = "",
    val createdByName: String = "",
    val enteredAt: Timestamp? = null,
    val serverWrittenAt: Timestamp? = null,
    val deletedAt: Timestamp? = null,
    val deletedBy: String? = null,
)
