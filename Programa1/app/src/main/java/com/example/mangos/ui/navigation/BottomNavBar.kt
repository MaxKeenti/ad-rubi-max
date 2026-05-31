package com.example.mangos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mangos.data.model.UserRole

@Composable
fun BottomNavBar(
    currentUserRole: UserRole,
    navController: NavHostController,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val items = bottomNavItemsFor(currentUserRole)

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentDestination
                    ?.hierarchy
                    ?.any { it.route == item.screen.route } == true,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}

private fun bottomNavItemsFor(role: UserRole): List<BottomNavItem> {
    val operatorItems = listOf(
        BottomNavItem(Screen.Dashboard, "Inicio", Icons.Filled.Home),
        BottomNavItem(Screen.Purchases, "Compras", Icons.Filled.LocalShipping),
        BottomNavItem(Screen.Reports, "Reportes", Icons.Filled.Assessment),
    )

    return if (role == UserRole.ADMIN) {
        operatorItems.toMutableList().apply {
            add(2, BottomNavItem(Screen.Suppliers, "Proveedores", Icons.Filled.People))
            add(3, BottomNavItem(Screen.Users, "Usuarios", Icons.Filled.ManageAccounts))
        }
    } else {
        operatorItems
    }
}

@Stable
private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)
