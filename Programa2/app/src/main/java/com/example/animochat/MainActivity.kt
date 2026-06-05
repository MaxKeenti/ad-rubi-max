package com.example.animochat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.animochat.ui.chat.ChatScreen
import com.example.animochat.ui.theme.AnimochatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimochatTheme {
                ChatScreen()
            }
        }
    }
}

@Composable
fun MainActivityPreview() {
    AnimochatTheme {
        ChatScreen()
    }
}
