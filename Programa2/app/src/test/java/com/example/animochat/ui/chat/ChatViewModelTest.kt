package com.example.animochat.ui.chat

import com.example.animochat.data.model.AiFeedback
import com.example.animochat.data.model.ChatMessage
import com.example.animochat.data.model.ChatRole
import com.example.animochat.data.model.FeedbackCategory
import com.example.animochat.data.repository.AiChatRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule
    internal val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sendMessage_ignoresSecondSendWhileLoading() = runTest(mainDispatcherRule.dispatcher) {
        val repository = BlockingAiChatRepository()
        val viewModel = ChatViewModel(repository)

        viewModel.sendMessage("Tengo miedo de reprobar matematicas")
        runCurrent()
        viewModel.sendMessage("Siento que decepcione a mis papas")

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(listOf("Tengo miedo de reprobar matematicas"), repository.receivedMessages)
        assertEquals(
            listOf("Tengo miedo de reprobar matematicas"),
            viewModel.uiState.value.messages
                .filter { it.role == ChatRole.STUDENT }
                .map { it.text }
        )

        repository.complete()
        runCurrent()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class BlockingAiChatRepository : AiChatRepository {
    val receivedMessages = mutableListOf<String>()
    private val response = CompletableDeferred<Result<AiFeedback>>()

    override suspend fun sendMessage(
        message: String,
        recentMessages: List<ChatMessage>,
    ): Result<AiFeedback> {
        receivedMessages += message
        return response.await()
    }

    fun complete() {
        response.complete(
            Result.success(
                AiFeedback(
                    category = FeedbackCategory.ACADEMIC_STRESS,
                    message = "Puedes avanzar con un paso pequeno.",
                    nextSteps = listOf("Estudia 10 minutos.")
                )
            )
        )
    }
}
