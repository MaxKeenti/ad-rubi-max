package com.example.animochat.data.ai.gemini

import com.example.animochat.data.ai.GeminiConfig
import com.example.animochat.data.model.FeedbackCategory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.test.runTest
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

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
        val thinkingBudget = root.getValue("generationConfig")
            .jsonObject
            .getValue("thinkingConfig")
            .jsonObject
            .getValue("thinkingBudget")
            .jsonPrimitive
            .content
        val maxOutputTokens = root.getValue("generationConfig")
            .jsonObject
            .getValue("maxOutputTokens")
            .jsonPrimitive
            .content

        assertTrue(systemInstruction.contains("Return only valid JSON"))
        assertTrue(systemInstruction.contains("killing or murder"))
        assertTrue(systemInstruction.contains("self-harm"))
        assertTrue(studentInput.contains("No se como estudiar para el examen."))
        assertEquals("application/json", responseMimeType)
        assertEquals("0", thinkingBudget)
        assertEquals("1024", maxOutputTokens)
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
    fun parseGenerateContentResponse_acceptsStringResourcesFromProvider() {
        val responseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"category\":\"academic_stress\",\"message\":\"Toma una pausa corta y vuelve a una tarea pequena.\",\"nextSteps\":[\"Descansa 10 minutos\"],\"resources\":[\"Student Wellness Center\"]}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val feedback = client.parseGenerateContentResponse(responseJson).getOrThrow()

        assertEquals(FeedbackCategory.ACADEMIC_STRESS, feedback.category)
        assertEquals("Student Wellness Center", feedback.resources.single().label)
        assertEquals("Student Wellness Center", feedback.resources.single().value)
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

    @Test
    fun generateFeedback_usesLatestFlashModelAndApiKeyHeader() = runTest {
        var capturedUrl = ""
        var capturedApiKeyHeader: String? = null
        val interceptingClient = OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    capturedUrl = chain.request().url.toString()
                    capturedApiKeyHeader = chain.request().header("X-goog-api-key")
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(
                            """
                            {
                              "candidates": [
                                {
                                  "content": {
                                    "parts": [
                                      {
                                        "text": "{\"category\":\"academic_stress\",\"message\":\"Puedes avanzar con un paso pequeno.\",\"nextSteps\":[\"Estudia 10 minutos\"]}"
                                      }
                                    ]
                                  }
                                }
                              ]
                            }
                            """.trimIndent().toResponseBody()
                        )
                        .build()
                }
            )
            .build()
        val client = GeminiRestClient(
            config = GeminiConfig(apiKey = "test-key"),
            httpClient = interceptingClient,
        )

        val result = client.generateFeedback("Necesito ayuda para estudiar.")

        assertTrue(result.isSuccess)
        assertTrue(capturedUrl.contains("/models/gemini-flash-latest:generateContent"))
        assertTrue(!capturedUrl.contains("key="))
        assertEquals("test-key", capturedApiKeyHeader)
    }

    @Test
    fun generateFeedback_returnsNetworkFailureWhenInternetPermissionDenied() = runTest {
        val permissionDeniedClient = GeminiRestClient(
            config = GeminiConfig(apiKey = "test-key"),
            httpClient = OkHttpClient.Builder()
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        throw SecurityException("Permission denied (missing INTERNET permission?)")
                    }
                })
                .build(),
        )

        val result = permissionDeniedClient.generateFeedback("Necesito ayuda para estudiar.")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is GeminiClientException.NetworkError)
        assertTrue(exception?.cause is SecurityException)
    }
}
