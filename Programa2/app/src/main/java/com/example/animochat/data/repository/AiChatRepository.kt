package com.example.animochat.data.repository

import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.ChatMessage

interface AiChatRepository {
    suspend fun sendMessage(
        message: String,
        recentMessages: List<ChatMessage>,
    ): Result<AiFeedback>
}
