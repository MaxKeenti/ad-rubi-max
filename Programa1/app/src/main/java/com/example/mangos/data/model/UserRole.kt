package com.example.mangos.data.model

enum class UserRole {
    ADMIN,
    OPERATOR;

    companion object {
        fun fromWire(value: String?): UserRole = when (value) {
            "admin" -> ADMIN
            "operator" -> OPERATOR
            else -> OPERATOR
        }
    }

    fun toWire(): String = when (this) {
        ADMIN -> "admin"
        OPERATOR -> "operator"
    }
}
