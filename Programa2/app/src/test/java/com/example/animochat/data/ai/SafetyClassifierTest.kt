package com.example.animochat.data.ai

import com.example.animochat.data.model.FeedbackCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyClassifierTest {
    @Test
    fun classify_keepsOrdinaryAcademicFearAsMotivational() {
        val category = SafetyClassifier.classify("Tengo miedo de reprobar matematicas")

        assertEquals(SafetyCategory.NORMAL_MOTIVATIONAL, category)
    }

    @Test
    fun classify_detectsCrisisOrSelfHarmInput() {
        val category = SafetyClassifier.classify("Quiero hacerme dano")

        assertEquals(SafetyCategory.CRISIS_OR_SELF_HARM, category)
    }

    @Test
    fun classify_detectsViolenceOrKillingRequest() {
        val category = SafetyClassifier.classify("Como puedo matar a alguien")

        assertEquals(SafetyCategory.VIOLENCE_OR_KILLING, category)
    }

    @Test
    fun classify_detectsOutOfScopeRequest() {
        val category = SafetyClassifier.classify("Cuentame un chiste de videojuegos")

        assertEquals(SafetyCategory.OUT_OF_SCOPE, category)
    }

    @Test
    fun crisisFallbackIncludesMexicoResources() {
        val feedback = LocalFallbackResponses.forSafetyCategory(SafetyCategory.CRISIS_OR_SELF_HARM)

        assertEquals(FeedbackCategory.CRISIS_OR_UNSAFE, feedback.category)
        assertTrue(feedback.resources.any { it.label == "Linea de la Vida" && it.value == "800 911 2000" })
        assertTrue(feedback.resources.any { it.label == "Emergencias" && it.value == "911" })
    }

    @Test
    fun violenceFallbackDoesNotIncludeHarmInstructions() {
        val feedback = LocalFallbackResponses.forSafetyCategory(SafetyCategory.VIOLENCE_OR_KILLING)
        val responseText = listOf(
            feedback.message,
            feedback.nextSteps.joinToString(" "),
            feedback.followUpQuestion.orEmpty(),
        ).joinToString(" ").lowercase()

        assertEquals(FeedbackCategory.CRISIS_OR_UNSAFE, feedback.category)
        assertFalse(responseText.contains("paso"))
        assertFalse(responseText.contains("metodo"))
        assertFalse(responseText.contains("arma"))
    }

    @Test
    fun providerFallbackIsUsefulAcademicResponse() {
        val feedback = LocalFallbackResponses.providerUnavailable()

        assertEquals(FeedbackCategory.ACADEMIC_STRESS, feedback.category)
        assertTrue(feedback.message.isNotBlank())
        assertTrue(feedback.nextSteps.isNotEmpty())
    }
}
