package com.example.mangos.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object Purchases : Screen("purchases")
    data object Suppliers : Screen("suppliers")
    data object Reports : Screen("reports")

    data class AddEditPurchase(val purchaseId: String? = null) : Screen(
        route = if (purchaseId == null) {
            ROUTE_CREATE
        } else {
            "$ROUTE_CREATE?purchaseId=$purchaseId"
        },
    ) {
        companion object {
            const val ARG_PURCHASE_ID = "purchaseId"
            const val ROUTE_CREATE = "add-edit-purchase"
            const val ROUTE_PATTERN = "$ROUTE_CREATE?$ARG_PURCHASE_ID={$ARG_PURCHASE_ID}"
        }
    }

    data class AddEditSupplier(val supplierId: String? = null) : Screen(
        route = if (supplierId == null) {
            ROUTE_CREATE
        } else {
            "$ROUTE_CREATE?supplierId=$supplierId"
        },
    ) {
        companion object {
            const val ARG_SUPPLIER_ID = "supplierId"
            const val ROUTE_CREATE = "add-edit-supplier"
            const val ROUTE_PATTERN = "$ROUTE_CREATE?$ARG_SUPPLIER_ID={$ARG_SUPPLIER_ID}"
        }
    }
}
