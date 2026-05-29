package com.example.mangos.data.model

import com.google.firebase.Timestamp

data class Supplier(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val location: String,
    val mangoVariety: String,
    val isActive: Boolean,
    val createdAt: Timestamp,
    val createdBy: String,
) {
    companion object {
        const val UNREGISTERED_ID: String = "UNREGISTERED"
    }
}
