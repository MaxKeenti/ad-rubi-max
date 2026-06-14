package com.example.bachewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bachewatch.ui.navigation.BacheWatchNavGraph
import com.example.bachewatch.ui.theme.BacheWatchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class  MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BacheWatchTheme {
                BacheWatchNavGraph()
            }
        }
    }
}
