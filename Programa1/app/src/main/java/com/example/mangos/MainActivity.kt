package com.example.mangos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.mangos.ui.navigation.MangosNavGraph
import com.example.mangos.ui.theme.MangosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MangosTheme(dynamicColor = false) {
                MangosNavGraph(navController = rememberNavController())
            }
        }
    }
}
