package com.example.bachewatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bachewatch.ui.mapa.MapaScreen
import com.example.bachewatch.ui.recientes.RecientesScreen
import com.example.bachewatch.ui.reportar.ReportarScreen

@Composable
fun BacheWatchNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Mapa.route) {
        composable(Screen.Mapa.route) {
            MapaScreen(
                onReportar = { navController.navigate(Screen.Reportar.route) },
                onRecientes = { navController.navigate(Screen.Recientes.route) },
            )
        }
        composable(Screen.Reportar.route) {
            ReportarScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Recientes.route) {
            RecientesScreen(onBack = { navController.popBackStack() })
        }
    }
}
