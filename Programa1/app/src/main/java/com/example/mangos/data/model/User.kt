package com.example.mangos.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.OPERATOR,
    val accountCreatedAt: Timestamp? = null,
)
