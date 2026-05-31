package com.example.mangos.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
    val accountCreatedAt: Timestamp,
    val disabledAt: Timestamp? = null,
    val retiredAt: Timestamp? = null,
    val promotedToUid: String? = null,
    val promotedFromUid: String? = null,
)
