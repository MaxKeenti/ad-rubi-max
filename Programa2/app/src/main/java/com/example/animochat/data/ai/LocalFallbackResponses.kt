package com.example.animochat.data.ai

import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.FeedbackCategory
import com.example.animochat.data.model.SupportResource

object LocalFallbackResponses {
    val mexicoCrisisResources: List<SupportResource> = listOf(
        SupportResource(label = "Linea de la Vida", value = "800 911 2000"),
        SupportResource(label = "Emergencias", value = "911"),
    )

    fun forSafetyCategory(category: SafetyCategory): AiFeedback =
        when (category) {
            SafetyCategory.NORMAL_MOTIVATIONAL -> providerUnavailable()
            SafetyCategory.CRISIS_OR_SELF_HARM -> crisisOrSelfHarm()
            SafetyCategory.VIOLENCE_OR_KILLING -> violenceOrKilling()
            SafetyCategory.OUT_OF_SCOPE -> outOfScope()
        }

    fun providerUnavailable(): AiFeedback = AiFeedback(
        category = FeedbackCategory.ACADEMIC_STRESS,
        message = "No pude obtener una respuesta del asistente en este momento, pero tu preocupacion si importa. Empieza por una accion pequena y concreta para recuperar control.",
        nextSteps = listOf(
            "Elige una sola tarea academica que puedas avanzar en 10 minutos.",
            "Anota que necesitas preguntar a tu profesor o a un companero.",
            "Intenta de nuevo cuando tengas conexion estable."
        ),
        followUpQuestion = "Que materia o entrega te preocupa mas ahora mismo?",
    )

    private fun crisisOrSelfHarm(): AiFeedback = AiFeedback(
        category = FeedbackCategory.CRISIS_OR_UNSAFE,
        message = "Siento que estes pasando por algo tan pesado. AnimoChat no puede atender emergencias, pero no tienes que manejarlo sin apoyo: busca a una persona de confianza que este cerca de ti ahora.",
        nextSteps = listOf(
            "Contacta a alguien de confianza y dile que necesitas compania.",
            "Si hay peligro inmediato, llama a emergencias."
        ),
        resources = mexicoCrisisResources,
    )

    private fun violenceOrKilling(): AiFeedback = AiFeedback(
        category = FeedbackCategory.CRISIS_OR_UNSAFE,
        message = "No puedo ayudar con instrucciones para hacer dano. Si sientes que alguien puede estar en peligro, alejate de la situacion y busca apoyo inmediato de una persona de confianza o de emergencias.",
        nextSteps = listOf(
            "Pausa la conversacion y busca a un adulto, docente o familiar de confianza.",
            "Si existe riesgo inmediato, llama a emergencias."
        ),
        resources = mexicoCrisisResources,
    )

    private fun outOfScope(): AiFeedback = AiFeedback(
        category = FeedbackCategory.OUT_OF_SCOPE,
        message = "Puedo ayudarte mejor con preocupaciones academicas, miedo a reprobar, organizacion de estudio o motivacion escolar.",
        nextSteps = listOf(
            "Escribe que materia, tarea o entrega te preocupa."
        ),
        followUpQuestion = "Que situacion academica quieres trabajar?"
    )
}
