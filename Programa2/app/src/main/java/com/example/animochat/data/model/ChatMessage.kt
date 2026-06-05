package com.example.animochat.data.model

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val createdAtMillis: Long,
)

enum class ChatRole(val promptName: String) {
    STUDENT("student"),
    ASSISTANT("assistant"),
}
