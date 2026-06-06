package com.example.animochat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.animochat.data.model.ChatMessage
import com.example.animochat.data.model.ChatRole
import com.example.animochat.data.repository.AiChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: AiChatRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var failedMessage: String? = null
    private var messageCounter = 0

    fun sendMessage(rawMessage: String) {
        val message = rawMessage.trim()
        if (message.isBlank() || _uiState.value.isLoading) return

        failedMessage = null
        val studentMessage = ChatMessage(
            id = nextMessageId("student"),
            role = ChatRole.STUDENT,
            text = message,
            createdAtMillis = System.currentTimeMillis()
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + studentMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        requestAssistantResponse(message)
    }

    fun retryLastMessage() {
        val message = failedMessage ?: return
        if (_uiState.value.isLoading) return

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        requestAssistantResponse(message)
    }

    fun clearChat() {
        failedMessage = null
        _uiState.value = ChatUiState()
    }

    private fun requestAssistantResponse(message: String) {
        viewModelScope.launch {
            val recentMessages = _uiState.value.messages
            repository.sendMessage(
                message = message,
                recentMessages = recentMessages
            ).fold(
                onSuccess = { feedback ->
                    failedMessage = null
                    val assistantMessage = ChatMessage(
                        id = nextMessageId("assistant"),
                        role = ChatRole.ASSISTANT,
                        text = feedback.message,
                        createdAtMillis = System.currentTimeMillis()
                    )

                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + assistantMessage,
                            latestFeedback = feedback,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { throwable ->
                    failedMessage = message
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "No se pudo conectar con el asistente. Intenta otra vez."
                        )
                    }
                }
            )
        }
    }

    private fun nextMessageId(prefix: String): String {
        messageCounter += 1
        return "$prefix-$messageCounter"
    }

    class Factory(
        private val repository: AiChatRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
