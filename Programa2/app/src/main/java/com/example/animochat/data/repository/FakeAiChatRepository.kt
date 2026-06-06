package com.example.animochat.data.repository

import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.ChatMessage
import com.example.animochat.data.model.FeedbackCategory
import kotlinx.coroutines.delay

class FakeAiChatRepository : AiChatRepository {
    override suspend fun sendMessage(
        message: String,
        recentMessages: List<ChatMessage>,
    ): Result<AiFeedback> {
        delay(650)

        val feedback = AiFeedback(
            category = FeedbackCategory.ACADEMIC_STRESS,
            message = "Te leo. Vamos a bajarlo a algo manejable: elige una sola parte pequena de esto y dale 10 minutos con calma.",
            nextSteps = listOf(
                "Escribe la tarea o materia que mas te preocupa.",
                "Divide el siguiente paso en una accion que puedas terminar hoy.",
                "Pide ayuda concreta si necesitas una fecha, ejemplo o explicacion."
            ),
            followUpQuestion = "Que es lo primero que necesitas entregar o estudiar?"
        )

        return Result.success(feedback)
    }
}
