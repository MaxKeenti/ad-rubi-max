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
import com.example.mangos.ui.dashboard.DashboardScreen
import com.example.mangos.ui.purchases.AddEditPurchaseScreen
import com.example.mangos.ui.purchases.PurchaseHistoryScreen
import com.example.mangos.ui.suppliers.AddEditSupplierScreen
import com.example.mangos.ui.suppliers.SupplierListScreen

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
                DashboardScreen(
                    onAddPurchaseClick = {
                        navController.navigate(Screen.AddEditPurchase().route)
                    },
                )
            }
            composable(Screen.Purchases.route) {
                PurchaseHistoryScreen(
                    onPurchaseClick = { purchaseId ->
                        navController.navigate(Screen.AddEditPurchase(purchaseId).route)
                    },
                )
            }
            composable(Screen.Suppliers.route) {
                SupplierListScreen(
                    onAddSupplierClick = {
                        navController.navigate(Screen.AddEditSupplier().route)
                    },
                    onSupplierClick = { supplierId ->
                        navController.navigate(Screen.AddEditSupplier(supplierId).route)
                    },
                )
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
            ) {
                AddEditPurchaseScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
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
            ) {
                AddEditSupplierScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
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
