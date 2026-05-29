package com.example.mangos.ui.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mangos.data.model.Purchase
import com.example.mangos.data.model.Supplier
import com.example.mangos.data.util.centavosToMxnString
import com.google.firebase.Timestamp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun PurchaseHistoryScreen(
    onPurchaseClick: (String) -> Unit,
    viewModel: PurchaseHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    PurchaseHistoryContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        canEdit = viewModel::canEdit,
        onSupplierFilterSelected = viewModel::onSupplierFilterSelected,
        onPurchaseClick = onPurchaseClick,
        onDeletePurchase = viewModel::softDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseHistoryContent(
    uiState: PurchaseHistoryUiState,
    snackbarHostState: SnackbarHostState,
    canEdit: (Purchase, com.example.mangos.data.model.User?) -> Boolean,
    onSupplierFilterSelected: (String?) -> Unit,
    onPurchaseClick: (String) -> Unit,
    onDeletePurchase: (Purchase) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de compras",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SupplierFilterRow(
                suppliers = uiState.suppliers,
                selectedSupplierId = uiState.selectedSupplierId,
                onSupplierFilterSelected = onSupplierFilterSelected,
            )

            if (uiState.purchases.isEmpty()) {
                EmptyHistory(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.purchases,
                        key = { it.id },
                    ) { purchase ->
                        PurchaseDismissItem(
                            purchase = purchase,
                            canEdit = canEdit(purchase, uiState.user),
                            onClick = { onPurchaseClick(purchase.id) },
                            onDelete = { onDeletePurchase(purchase) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierFilterRow(
    suppliers: List<Supplier>,
    selectedSupplierId: String?,
    onSupplierFilterSelected: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedSupplierId == null,
                onClick = { onSupplierFilterSelected(null) },
                label = { Text("Todos") },
            )
        }
        items(
            items = suppliers,
            key = { it.id },
        ) { supplier ->
            FilterChip(
                selected = selectedSupplierId == supplier.id,
                onClick = { onSupplierFilterSelected(supplier.id) },
                label = {
                    Text(
                        text = supplier.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDismissItem(
    purchase: Purchase,
    canEdit: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = canEdit,
        enableDismissFromEndToStart = canEdit,
        backgroundContent = {
            DeleteBackground()
        },
    ) {
        PurchaseCard(
            purchase = purchase,
            canEdit = canEdit,
            onClick = onClick,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun PurchaseCard(
    purchase: Purchase,
    canEdit: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEdit, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = purchase.displaySupplierName(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${formatQuantity(purchase.quantityTons)} t - " +
                            purchase.pricePerTonCentavos.centavosToMxnString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${formatDate(purchase.date)} - Capturada ${formatDateTime(purchase.enteredAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailingContent = {
                Row {
                    IconButton(
                        enabled = canEdit,
                        onClick = onClick,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar compra",
                        )
                    }
                    IconButton(
                        enabled = canEdit,
                        onClick = onDelete,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Eliminar compra",
                            tint = if (canEdit) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color.Unspecified
                            },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sin compras registradas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cuando captures compras apareceran aqui.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun Purchase.displaySupplierName(): String =
    if (supplierId == Supplier.UNREGISTERED_ID) {
        supplierNoteFreeform?.takeIf { it.isNotBlank() } ?: supplierName
    } else {
        supplierName
    }

private fun formatQuantity(quantity: Double): String =
    if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        "%.2f".format(Locale.US, quantity).trimEnd('0').trimEnd('.')
    }

private fun formatDate(timestamp: Timestamp): String =
    DATE_FORMATTER.format(timestamp.toInstant().atZone(MX_ZONE))

private fun formatDateTime(timestamp: Timestamp): String =
    DATE_TIME_FORMATTER.format(timestamp.toInstant().atZone(MX_ZONE))

private val MX_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.forLanguageTag("es-MX"))
private val DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.forLanguageTag("es-MX"))
