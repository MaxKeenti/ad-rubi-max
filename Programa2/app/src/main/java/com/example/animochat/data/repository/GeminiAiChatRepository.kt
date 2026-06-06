package com.example.animochat.data.repository

import com.example.animochat.data.ai.GeminiConfig
import com.example.animochat.data.ai.PromptPolicy
import com.example.animochat.data.ai.gemini.GeminiClientException
import com.example.animochat.data.ai.gemini.GeminiRestClient
import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.ChatMessage
import com.example.animochat.data.model.FeedbackCategory

class GeminiAiChatRepository(
    private val client: GeminiRestClient = GeminiRestClient(GeminiConfig.fromBuildConfig()),
) : AiChatRepository {
    override suspend fun sendMessage(
        message: String,
        recentMessages: List<ChatMessage>,
    ): Result<AiFeedback> {
        val prompt = PromptPolicy.buildStudentPrompt(
            message = message,
            recentMessages = recentMessages,
        )

        return client.generateFeedback(prompt)
            .recoverCatching { throwable ->
                when (throwable) {
                    is GeminiClientException.MissingApiKey -> throw throwable
                    is GeminiClientException.MalformedProviderResponse -> fallbackFeedback()
                    is GeminiClientException.HttpError -> fallbackFeedback()
                    is GeminiClientException.NetworkError -> fallbackFeedback()
                    else -> fallbackFeedback()
                }
            }
    }

    private fun fallbackFeedback(): AiFeedback = AiFeedback(
        category = FeedbackCategory.ACADEMIC_STRESS,
        message = "No pude obtener una respuesta del asistente en este momento, pero tu preocupacion si importa. Empieza por una accion pequena y concreta para recuperar control.",
        nextSteps = listOf(
            "Elige una sola tarea academica que puedas avanzar en 10 minutos.",
            "Anota que necesitas preguntar a tu profesor o a un companero.",
            "Intenta de nuevo cuando tengas conexion estable."
        ),
        followUpQuestion = "Que materia o entrega te preocupa mas ahora mismo?",
    )
}
