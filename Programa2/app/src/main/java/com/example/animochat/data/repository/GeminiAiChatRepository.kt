package com.example.animochat.data.repository

import com.example.animochat.data.ai.GeminiConfig
import com.example.animochat.data.ai.LocalFallbackResponses
import com.example.animochat.data.ai.PromptPolicy
import com.example.animochat.data.ai.SafetyCategory
import com.example.animochat.data.ai.SafetyClassifier
import com.example.animochat.data.ai.gemini.GeminiClientException
import com.example.animochat.data.ai.gemini.GeminiRestClient
import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.ChatMessage

class GeminiAiChatRepository(
    private val client: GeminiRestClient = GeminiRestClient(GeminiConfig.fromBuildConfig()),
) : AiChatRepository {
    override suspend fun sendMessage(
        message: String,
        recentMessages: List<ChatMessage>,
    ): Result<AiFeedback> {
        val safetyCategory = SafetyClassifier.classify(message)
        if (safetyCategory != SafetyCategory.NORMAL_MOTIVATIONAL) {
            return Result.success(LocalFallbackResponses.forSafetyCategory(safetyCategory))
        }

        val prompt = PromptPolicy.buildStudentPrompt(
            message = message,
            recentMessages = recentMessages,
        )

        return client.generateFeedback(prompt)
            .recoverCatching { throwable ->
                when (throwable) {
                    is GeminiClientException.MissingApiKey -> throw throwable
                    is GeminiClientException.MalformedProviderResponse -> LocalFallbackResponses.providerUnavailable()
                    is GeminiClientException.HttpError -> LocalFallbackResponses.providerUnavailable()
                    is GeminiClientException.NetworkError -> LocalFallbackResponses.providerUnavailable()
                    else -> LocalFallbackResponses.providerUnavailable()
                }
            }
    }
}
