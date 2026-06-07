package com.example.animochat.data.ai

import com.example.animochat.BuildConfig

data class GeminiConfig(
    val apiKey: String,
    val model: String = DEFAULT_MODEL,
    val endpointBaseUrl: String = DEFAULT_ENDPOINT_BASE_URL,
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_MODEL = "gemini-flash-latest"
        const val DEFAULT_ENDPOINT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

        fun fromBuildConfig(): GeminiConfig = GeminiConfig(
            apiKey = BuildConfig.GEMINI_API_KEY,
        )
    }
}
