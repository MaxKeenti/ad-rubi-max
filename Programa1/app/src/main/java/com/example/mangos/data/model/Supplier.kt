package com.example.mangos.data.model

import com.google.firebase.Timestamp

data class Supplier(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val location: String = "",
    val mangoVariety: String = "",
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val createdBy: String = "",
) {
    companion object {
        const val UNREGISTERED_ID: String = "UNREGISTERED"
        const val UNREGISTERED_NAME: String = "Proveedor no registrado"
    }
}
