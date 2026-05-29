package com.example.mangos.ui.suppliers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AddEditSupplierScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEditSupplierViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            onSaved()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    AddEditSupplierContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onNameChanged = viewModel::onNameChanged,
        onPhoneChanged = viewModel::onPhoneChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onLocationChanged = viewModel::onLocationChanged,
        onMangoVarietyChanged = viewModel::onMangoVarietyChanged,
        onActiveChanged = viewModel::onActiveChanged,
        onNameBlurred = viewModel::onNameBlurred,
        onEmailBlurred = viewModel::onEmailBlurred,
        onLocationBlurred = viewModel::onLocationBlurred,
        onMangoVarietyBlurred = viewModel::onMangoVarietyBlurred,
        onSaveClick = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditSupplierContent(
    uiState: AddEditSupplierUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onMangoVarietyChanged: (String) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onNameBlurred: () -> Unit,
    onEmailBlurred: () -> Unit,
    onLocationBlurred: () -> Unit,
    onMangoVarietyBlurred: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val title = if (uiState.isEditMode) "Editar proveedor" else "Registrar proveedor"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) onNameBlurred()
                        },
                    enabled = uiState.canSave,
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = onPhoneChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canSave,
                    label = { Text("Telefono") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    supportingText = { Text("Opcional: 55 1234 5678") },
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) onEmailBlurred()
                        },
                    enabled = uiState.canSave,
                    label = { Text("Correo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = uiState.emailError != null,
                    supportingText = { Text(uiState.emailError ?: "Opcional") },
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.location,
                    onValueChange = onLocationChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) onLocationBlurred()
                        },
                    enabled = uiState.canSave,
                    label = { Text("Ubicacion") },
                    singleLine = true,
                    isError = uiState.locationError != null,
                    supportingText = uiState.locationError?.let { { Text(it) } },
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.mangoVariety,
                    onValueChange = onMangoVarietyChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) onMangoVarietyBlurred()
                        },
                    enabled = uiState.canSave,
                    label = { Text("Variedad de mango") },
                    singleLine = true,
                    isError = uiState.mangoVarietyError != null,
                    supportingText = uiState.mangoVarietyError?.let { { Text(it) } },
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (uiState.isActive) {
                                "Disponible para nuevas compras"
                            } else {
                                "Oculto en selecciones activas"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        checked = uiState.isActive,
                        onCheckedChange = onActiveChanged,
                        enabled = uiState.canSave,
                    )
                }
            }

            item {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canSave,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isSaving) "Guardando..." else "Guardar")
                }
            }
        }
    }
}
