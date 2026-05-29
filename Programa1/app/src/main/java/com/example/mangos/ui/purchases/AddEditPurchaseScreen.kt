package com.example.mangos.ui.purchases

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mangos.data.model.Supplier
import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun AddEditPurchaseScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEditPurchaseViewModel = hiltViewModel(),
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

    AddEditPurchaseContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onSupplierSelected = viewModel::onSupplierSelected,
        onSupplierNoteChanged = viewModel::onSupplierNoteChanged,
        onQuantityChanged = viewModel::onQuantityChanged,
        onQuantityBlurred = viewModel::onQuantityBlurred,
        onPriceChanged = viewModel::onPriceChanged,
        onPriceBlurred = viewModel::onPriceBlurred,
        onDateSelected = viewModel::onDateSelected,
        onSaveClick = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditPurchaseContent(
    uiState: AddEditPurchaseUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onSupplierSelected: (String) -> Unit,
    onSupplierNoteChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onQuantityBlurred: () -> Unit,
    onPriceChanged: (String) -> Unit,
    onPriceBlurred: () -> Unit,
    onDateSelected: (Timestamp) -> Unit,
    onSaveClick: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val title = if (uiState.isEditMode) "Editar compra" else "Registrar compra"
    val saveEnabled = !uiState.isSaving &&
        !uiState.isLoading &&
        uiState.canEditCurrentPurchase

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
            if (uiState.editBlockedMessage != null) {
                item {
                    Text(
                        text = uiState.editBlockedMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            item {
                SupplierDropdown(
                    suppliers = uiState.suppliers,
                    selectedSupplierId = uiState.selectedSupplierId,
                    enabled = saveEnabled,
                    onSupplierSelected = onSupplierSelected,
                )
            }

            if (uiState.isUnregisteredSupplier) {
                item {
                    OutlinedTextField(
                        value = uiState.supplierNoteFreeform,
                        onValueChange = onSupplierNoteChanged,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = saveEnabled,
                        label = { Text("Nombre o referencia del proveedor") },
                        singleLine = true,
                        isError = uiState.supplierNoteError != null,
                        supportingText = uiState.supplierNoteError?.let { { Text(it) } },
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.quantityTons,
                    onValueChange = onQuantityChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) onQuantityBlurred()
                        },
                    enabled = saveEnabled,
                    label = { Text("Toneladas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.quantityError != null,
                    supportingText = uiState.quantityError?.let { { Text(it) } },
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.pricePerTonMxn,
                    onValueChange = onPriceChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) onPriceBlurred()
                        },
                    enabled = saveEnabled,
                    label = { Text("Precio por tonelada (MXN)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.priceError != null,
                    supportingText = {
                        Text(uiState.priceError ?: "Opcional")
                    },
                )
            }

            item {
                OutlinedTextField(
                    value = formatDate(uiState.date),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = saveEnabled,
                    readOnly = true,
                    label = { Text("Fecha de recepción") },
                    trailingIcon = {
                        IconButton(
                            enabled = saveEnabled,
                            onClick = { showDatePicker = true },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Elegir fecha",
                            )
                        }
                    },
                )
            }

            item {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = saveEnabled,
                ) {
                    Text(if (uiState.isSaving) "Guardando..." else "Guardar")
                }
            }
        }
    }

    if (showDatePicker) {
        PurchaseDatePickerDialog(
            initialDate = uiState.date,
            onDismiss = { showDatePicker = false },
            onDateSelected = { timestamp ->
                onDateSelected(timestamp)
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierDropdown(
    suppliers: List<Supplier>,
    selectedSupplierId: String,
    enabled: Boolean,
    onSupplierSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSupplier = suppliers.firstOrNull { it.id == selectedSupplierId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (enabled) expanded = !expanded
        },
    ) {
        OutlinedTextField(
            value = selectedSupplier?.name.orEmpty(),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled,
            readOnly = true,
            label = { Text("Proveedor") },
            trailingIcon = {
                Row {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            suppliers.filter { it.id != Supplier.UNREGISTERED_ID }.forEach { supplier ->
                DropdownMenuItem(
                    text = { Text(supplier.name) },
                    onClick = {
                        onSupplierSelected(supplier.id)
                        expanded = false
                    },
                )
            }
            if (suppliers.any { it.id == Supplier.UNREGISTERED_ID }) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Proveedor no registrado") },
                    onClick = {
                        onSupplierSelected(Supplier.UNREGISTERED_ID)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDatePickerDialog(
    initialDate: Timestamp,
    onDismiss: () -> Unit,
    onDateSelected: (Timestamp) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toPickerMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.let { onDateSelected(it.toMexicoTimestamp()) }
                        ?: onDismiss()
                },
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun Timestamp.toPickerMillis(): Long {
    val localDate = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
        .atZone(MX_ZONE)
        .toLocalDate()
    return localDate.atStartOfDay(UTC_ZONE).toInstant().toEpochMilli()
}

private fun Long.toMexicoTimestamp(): Timestamp {
    val localDate = Instant.ofEpochMilli(this).atZone(UTC_ZONE).toLocalDate()
    val instant = localDate.atStartOfDay(MX_ZONE).toInstant()
    return Timestamp(instant.epochSecond, instant.nano)
}

private fun formatDate(timestamp: Timestamp): String {
    val localDate = Instant.ofEpochSecond(timestamp.seconds, timestamp.nanoseconds.toLong())
        .atZone(MX_ZONE)
        .toLocalDate()
    return DATE_FORMATTER.format(localDate)
}

private val MX_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
private val UTC_ZONE: ZoneId = ZoneId.of("UTC")
private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.forLanguageTag("es-MX"))
