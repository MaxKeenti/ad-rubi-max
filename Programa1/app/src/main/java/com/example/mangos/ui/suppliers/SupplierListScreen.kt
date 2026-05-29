package com.example.mangos.ui.suppliers

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mangos.data.model.Supplier

@Composable
fun SupplierListScreen(
    onAddSupplierClick: () -> Unit,
    onSupplierClick: (String) -> Unit,
    viewModel: SupplierListViewModel = hiltViewModel(),
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

    SupplierListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddSupplierClick = onAddSupplierClick,
        onSupplierClick = onSupplierClick,
        onDeactivateSupplier = viewModel::deactivate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierListContent(
    uiState: SupplierListUiState,
    snackbarHostState: SnackbarHostState,
    onAddSupplierClick: () -> Unit,
    onSupplierClick: (String) -> Unit,
    onDeactivateSupplier: (Supplier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Proveedores",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSupplierClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Registrar proveedor",
                )
            }
        },
    ) { innerPadding ->
        if (uiState.suppliers.isEmpty()) {
            EmptySuppliers(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = uiState.suppliers,
                    key = { it.id },
                ) { supplier ->
                    SupplierDismissItem(
                        supplier = supplier,
                        onClick = { onSupplierClick(supplier.id) },
                        onDeactivate = { onDeactivateSupplier(supplier) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierDismissItem(
    supplier: Supplier,
    onClick: () -> Unit,
    onDeactivate: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && supplier.isActive) {
                onDeactivate()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = supplier.isActive,
        enableDismissFromEndToStart = supplier.isActive,
        backgroundContent = {
            DeactivateBackground()
        },
    ) {
        SupplierCard(
            supplier = supplier,
            onClick = onClick,
            onDeactivate = onDeactivate,
        )
    }
}

@Composable
private fun DeactivateBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun SupplierCard(
    supplier: Supplier,
    onClick: () -> Unit,
    onDeactivate: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = supplier.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = supplier.location.ifBlank { "Sin ubicacion" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = supplier.mangoVariety.ifBlank { "Variedad sin capturar" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SupplierStatusChip(isActive = supplier.isActive)
                    IconButton(
                        enabled = supplier.isActive,
                        onClick = onDeactivate,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = "Desactivar proveedor",
                            tint = if (supplier.isActive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar proveedor",
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun SupplierStatusChip(isActive: Boolean) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = if (isActive) "Activo" else "Inactivo",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        enabled = false,
    )
}

@Composable
private fun EmptySuppliers(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sin proveedores registrados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Agrega proveedores para usarlos en nuevas compras.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
