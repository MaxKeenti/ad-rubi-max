package com.example.animochat.data.ai.gemini

import com.example.animochat.data.ai.GeminiConfig
import com.example.animochat.data.ai.PromptPolicy
import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.FeedbackCategory
import com.example.animochat.data.model.SupportResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class GeminiRestClient(
    private val config: GeminiConfig,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val json: Json = defaultJson,
) {
    suspend fun generateFeedback(
        studentPrompt: String,
    ): Result<AiFeedback> = withContext(Dispatchers.IO) {
        if (!config.hasApiKey) {
            return@withContext Result.failure(GeminiClientException.MissingApiKey)
        }

        val request = Request.Builder()
            .url(buildEndpointUrl(config))
            .post(buildRequestJson(studentPrompt).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        GeminiClientException.HttpError(response.code, body)
                    )
                }
                parseGenerateContentResponse(body)
            }
        } catch (exception: IOException) {
            Result.failure(GeminiClientException.NetworkError(exception))
        }
    }

    fun buildRequestJson(studentPrompt: String): String {
        val request = GeminiGenerateContentRequest(
            systemInstruction = GeminiContent(
                parts = listOf(GeminiTextPart(PromptPolicy.systemInstruction)),
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiTextPart(studentPrompt)),
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4,
                maxOutputTokens = 600,
            ),
        )

        return json.encodeToString(request)
    }

    fun parseGenerateContentResponse(responseJson: String): Result<AiFeedback> {
        return try {
            val response = json.decodeFromString<GeminiGenerateContentResponse>(responseJson)
            val providerText = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstNotNullOfOrNull { it.text }
                .orEmpty()

            parseFeedbackJson(providerText)
        } catch (exception: SerializationException) {
            Result.failure(GeminiClientException.MalformedProviderResponse(exception))
        } catch (exception: IllegalArgumentException) {
            Result.failure(GeminiClientException.MalformedProviderResponse(exception))
        }
    }

    fun parseFeedbackJson(providerText: String): Result<AiFeedback> {
        val feedbackJson = providerText.extractJsonObject()
            ?: return Result.failure(GeminiClientException.MalformedProviderResponse())

        return try {
            val dto = json.decodeFromString<AiFeedbackDto>(feedbackJson)
            val category = FeedbackCategory.fromJsonName(dto.category)
                ?: return Result.failure(GeminiClientException.MalformedProviderResponse())
            val message = dto.message.trim()
            if (message.isBlank()) {
                return Result.failure(GeminiClientException.MalformedProviderResponse())
            }

            Result.success(
                AiFeedback(
                    category = category,
                    message = message,
                    nextSteps = dto.nextSteps.map { it.trim() }.filter { it.isNotBlank() }.take(3),
                    followUpQuestion = dto.followUpQuestion?.trim()?.takeIf { it.isNotBlank() },
                    resources = dto.resources.mapNotNull { resource ->
                        val label = resource.label.trim()
                        val value = resource.value.trim()
                        if (label.isBlank() || value.isBlank()) {
                            null
                        } else {
                            SupportResource(label = label, value = value)
                        }
                    },
                )
            )
        } catch (exception: SerializationException) {
            Result.failure(GeminiClientException.MalformedProviderResponse(exception))
        }
    }

    private fun buildEndpointUrl(config: GeminiConfig): String {
        val encodedModel = URLEncoder.encode(config.model, StandardCharsets.UTF_8.name())
        val encodedKey = URLEncoder.encode(config.apiKey, StandardCharsets.UTF_8.name())
        return "${config.endpointBaseUrl}/models/$encodedModel:generateContent?key=$encodedKey"
    }

    private fun String.extractJsonObject(): String? {
        val start = indexOf('{')
        val end = lastIndexOf('}')
        return if (start >= 0 && end > start) substring(start, end + 1) else null
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

sealed class GeminiClientException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object MissingApiKey : GeminiClientException("Gemini API key is missing.")

    data class HttpError(
        val statusCode: Int,
        val responseBody: String,
    ) : GeminiClientException("Gemini HTTP request failed with status $statusCode.")

    data class NetworkError(
        override val cause: IOException,
    ) : GeminiClientException("Gemini network request failed.", cause)

    data class MalformedProviderResponse(
        override val cause: Throwable? = null,
    ) : GeminiClientException("Gemini returned malformed or incomplete JSON.", cause)
}

@Serializable
private data class GeminiGenerateContentRequest(
    @SerialName("systemInstruction")
    val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig,
)

@Serializable
private data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiTextPart>,
)

@Serializable
private data class GeminiTextPart(
    val text: String,
)

@Serializable
private data class GeminiGenerationConfig(
    @SerialName("responseMimeType")
    val responseMimeType: String,
    val temperature: Double,
    @SerialName("maxOutputTokens")
    val maxOutputTokens: Int,
)

@Serializable
private data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiResponseContent? = null,
)

@Serializable
private data class GeminiResponseContent(
    val parts: List<GeminiResponsePart> = emptyList(),
)

@Serializable
private data class GeminiResponsePart(
    val text: String? = null,
)

@Serializable
private data class AiFeedbackDto(
    val category: String,
    val message: String,
    val nextSteps: List<String> = emptyList(),
    val followUpQuestion: String? = null,
    val resources: List<SupportResourceDto> = emptyList(),
)

@Serializable
private data class SupportResourceDto(
    val label: String,
    val value: String,
)
