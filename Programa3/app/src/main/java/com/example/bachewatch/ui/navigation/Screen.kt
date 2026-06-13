package com.example.bachewatch.ui.navigation

/** Map-first screen map (grilling Q13). */
sealed class Screen(val route: String) {
    data object Mapa : Screen("mapa")
    data object Reportar : Screen("reportar")
    data object Recientes : Screen("recientes")
}
