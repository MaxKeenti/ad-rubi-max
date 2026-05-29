package com.example.mangos.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mangos.data.model.User
import com.example.mangos.ui.auth.LoginScreen

@Composable
fun MangosNavGraph(
    navController: NavHostController,
    viewModel: MangosNavViewModel = hiltViewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsState()

    key(currentUser?.id) {
        if (currentUser == null) {
            LoginGraph(navController = navController)
        } else {
            AuthedGraph(
                navController = navController,
                currentUser = checkNotNull(currentUser),
            )
        }
    }
}

@Composable
private fun LoginGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
    ) {
        composable(Screen.Login.route) {
            LoginScreen()
        }
    }
}

@Composable
private fun AuthedGraph(
    navController: NavHostController,
    currentUser: User,
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentUserRole = currentUser.role,
                navController = navController,
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Dashboard.route) {
                PlaceholderScreen("Inicio")
            }
            composable(Screen.Purchases.route) {
                PlaceholderScreen("Compras")
            }
            composable(Screen.Suppliers.route) {
                PlaceholderScreen("Proveedores")
            }
            composable(Screen.Reports.route) {
                PlaceholderScreen("Reportes")
            }
            composable(
                route = Screen.AddEditPurchase.ROUTE_PATTERN,
                arguments = listOf(
                    navArgument(Screen.AddEditPurchase.ARG_PURCHASE_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val purchaseId = backStackEntry.arguments
                    ?.getString(Screen.AddEditPurchase.ARG_PURCHASE_ID)
                PlaceholderScreen(
                    text = if (purchaseId == null) {
                        "Registrar compra"
                    } else {
                        "Editar compra"
                    },
                )
            }
            composable(
                route = Screen.AddEditSupplier.ROUTE_PATTERN,
                arguments = listOf(
                    navArgument(Screen.AddEditSupplier.ARG_SUPPLIER_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments
                    ?.getString(Screen.AddEditSupplier.ARG_SUPPLIER_ID)
                PlaceholderScreen(
                    text = if (supplierId == null) {
                        "Registrar proveedor"
                    } else {
                        "Editar proveedor"
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
