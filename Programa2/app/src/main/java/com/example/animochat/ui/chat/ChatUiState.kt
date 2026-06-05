package com.example.animochat.ui.chat

import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.ChatMessage
import com.example.animochat.data.model.ChatRole
import com.example.animochat.data.model.FeedbackCategory
import com.example.animochat.data.model.SupportResource

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val latestFeedback: AiFeedback? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val canRetry: Boolean
        get() = errorMessage != null && !isLoading
}

internal fun fakeChatUiState(): ChatUiState {
    val feedback = AiFeedback(
        category = FeedbackCategory.CRISIS_OR_UNSAFE,
        message = "Siento que estes pasando por un momento tan pesado. No tienes que resolverlo a solas; lo mas importante ahora es mantenerte a salvo y buscar apoyo inmediato.",
        nextSteps = listOf(
            "Respira lento durante un minuto y alejate de cualquier cosa con la que puedas hacerte dano.",
            "Escribe o llama a una persona de confianza y dile que necesitas compania ahora.",
            "Contacta a emergencias o a una linea de crisis si sientes que puedes lastimarte."
        ),
        followUpQuestion = "Puedes decirme si estas en un lugar seguro en este momento?",
        resources = listOf(
            SupportResource(
                label = "Emergencias",
                value = "911"
            ),
            SupportResource(
                label = "Apoyo en crisis",
                value = "Busca la linea local de intervencion en crisis de tu ciudad o universidad."
            )
        )
    )

    return ChatUiState(
        messages = listOf(
            ChatMessage(
                id = "student-1",
                role = ChatRole.STUDENT,
                text = "Me siento muy atrasada con todo y ya no se por donde empezar.",
                createdAtMillis = 0L
            ),
            ChatMessage(
                id = "assistant-1",
                role = ChatRole.ASSISTANT,
                text = feedback.message,
                createdAtMillis = 1L
            )
        ),
        latestFeedback = feedback,
        errorMessage = "No se pudo conectar con el asistente. Revisa tu conexion e intenta otra vez."
    )
}
