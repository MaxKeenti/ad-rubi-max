package com.example.animochat.data.ai.gemini

import com.example.animochat.data.ai.GeminiConfig
import com.example.animochat.data.model.FeedbackCategory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRestClientTest {
    private val client = GeminiRestClient(
        config = GeminiConfig(apiKey = "test-key"),
    )

    @Test
    fun buildRequestJson_includesSystemInstructionStudentInputAndNegativeCases() {
        val requestJson = client.buildRequestJson(
            studentPrompt = """
                Recent conversation:
                student: Tengo miedo de reprobar matematicas.

                Latest student message:
                No se como estudiar para el examen.
            """.trimIndent()
        )

        val root = Json.parseToJsonElement(requestJson).jsonObject
        val systemInstruction = root.getValue("systemInstruction")
            .jsonObject
            .getValue("parts")
            .jsonArray
            .first()
            .jsonObject
            .getValue("text")
            .jsonPrimitive
            .content
        val studentInput = root.getValue("contents")
            .jsonArray
            .first()
            .jsonObject
            .getValue("parts")
            .jsonArray
            .first()
            .jsonObject
            .getValue("text")
            .jsonPrimitive
            .content
        val responseMimeType = root.getValue("generationConfig")
            .jsonObject
            .getValue("responseMimeType")
            .jsonPrimitive
            .content

        assertTrue(systemInstruction.contains("Return only valid JSON"))
        assertTrue(systemInstruction.contains("killing or murder"))
        assertTrue(systemInstruction.contains("self-harm"))
        assertTrue(studentInput.contains("No se como estudiar para el examen."))
        assertEquals("application/json", responseMimeType)
    }

    @Test
    fun parseGenerateContentResponse_mapsProviderJsonToAiFeedback() {
        val responseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"category\":\"academic_stress\",\"message\":\"Respira, puedes avanzar paso a paso.\",\"nextSteps\":[\"Haz una lista corta\",\"Estudia 20 minutos\"],\"followUpQuestion\":\"Que tema te cuesta mas?\",\"resources\":[{\"label\":\"Tutorias\",\"value\":\"Consulta a tu profesor\"}]}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val feedback = client.parseGenerateContentResponse(responseJson).getOrThrow()

        assertEquals(FeedbackCategory.ACADEMIC_STRESS, feedback.category)
        assertEquals("Respira, puedes avanzar paso a paso.", feedback.message)
        assertEquals(listOf("Haz una lista corta", "Estudia 20 minutos"), feedback.nextSteps)
        assertEquals("Que tema te cuesta mas?", feedback.followUpQuestion)
        assertEquals("Tutorias", feedback.resources.single().label)
    }

    @Test
    fun parseGenerateContentResponse_returnsFailureForMalformedProviderJson() {
        val responseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Esto no es JSON valido" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = client.parseGenerateContentResponse(responseJson)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GeminiClientException.MalformedProviderResponse)
    }
}
