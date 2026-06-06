package com.example.animochat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.animochat.data.ai.GeminiConfig
import com.example.animochat.data.repository.FakeAiChatRepository
import com.example.animochat.data.repository.GeminiAiChatRepository
import com.example.animochat.ui.chat.ChatRoute
import com.example.animochat.ui.chat.ChatScreen
import com.example.animochat.ui.chat.ChatViewModel
import com.example.animochat.ui.chat.fakeChatUiState
import com.example.animochat.ui.theme.AnimochatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimochatTheme {
                val repository = remember {
                    if (GeminiConfig.fromBuildConfig().hasApiKey) {
                        GeminiAiChatRepository()
                    } else {
                        FakeAiChatRepository()
                    }
                }
                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModel.Factory(repository)
                )
                ChatRoute(viewModel = chatViewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    AnimochatTheme {
        ChatScreen(
            state = fakeChatUiState(),
            onSendMessage = {},
            onRetry = {},
            onClear = {}
        )
    }
}
