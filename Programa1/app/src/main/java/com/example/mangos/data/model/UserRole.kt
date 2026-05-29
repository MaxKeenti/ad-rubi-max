package com.example.mangos.data.model

enum class UserRole {
    ADMIN,
    OPERATOR;

    companion object {
        fun fromFirestoreString(s: String?): UserRole = when (s) {
            "admin" -> ADMIN
            "operator" -> OPERATOR
            else -> OPERATOR
        }
    }
}
