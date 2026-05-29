package com.example.mangos.ui.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangos.data.model.Purchase
import com.example.mangos.data.model.Supplier
import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.PurchaseRepository
import com.example.mangos.data.repository.SupplierRepository
import com.example.mangos.data.util.parseMxnToCentavos
import com.example.mangos.data.util.toDateKey
import com.example.mangos.ui.navigation.Screen
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditPurchaseUiState(
    val purchaseId: String? = null,
    val user: User? = null,
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: String = "",
    val supplierNoteFreeform: String = "",
    val quantityTons: String = "",
    val pricePerTonMxn: String = "",
    val date: Timestamp = todayTimestamp(),
    val quantityError: String? = null,
    val priceError: String? = null,
    val supplierNoteError: String? = null,
    val canEditCurrentPurchase: Boolean = true,
    val editBlockedMessage: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEditMode: Boolean = purchaseId != null
    val isUnregisteredSupplier: Boolean = selectedSupplierId == Supplier.UNREGISTERED_ID
}

@HiltViewModel
class AddEditPurchaseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val supplierRepository: SupplierRepository,
    private val purchaseRepository: PurchaseRepository,
) : ViewModel() {

    private val purchaseId: String? =
        savedStateHandle[Screen.AddEditPurchase.ARG_PURCHASE_ID]

    private val formState = MutableStateFlow(
        AddEditPurchaseUiState(
            purchaseId = purchaseId,
            isLoading = purchaseId != null,
        )
    )

    private var loadedPurchase: Purchase? = null

    val uiState: StateFlow<AddEditPurchaseUiState> = combine(
        formState,
        authRepository.currentUser,
        supplierRepository.observeActive(),
    ) { form, user, suppliers ->
        val orderedSuppliers = suppliers.sortedWith(
            compareBy<Supplier> { it.id == Supplier.UNREGISTERED_ID }
                .thenBy { it.name.lowercase() }
        )
        val selectedSupplierId = form.selectedSupplierId.ifBlank {
            orderedSuppliers.firstOrNull { it.id != Supplier.UNREGISTERED_ID }?.id
                ?: orderedSuppliers.firstOrNull()?.id
                ?: ""
        }
        form.copy(
            user = user,
            suppliers = orderedSuppliers,
            selectedSupplierId = selectedSupplierId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = formState.value,
    )

    init {
        if (purchaseId != null) {
            viewModelScope.launch {
                val purchase = purchaseRepository.observeById(purchaseId)
                    .filterNotNull()
                    .first()
                val user = authRepository.currentUser.filterNotNull().first()
                loadedPurchase = purchase
                formState.update {
                    it.copy(
                        selectedSupplierId = purchase.supplierId,
                        supplierNoteFreeform = purchase.supplierNoteFreeform.orEmpty(),
                        quantityTons = formatEditableDouble(purchase.quantityTons),
                        pricePerTonMxn = purchase.pricePerTonCentavos
                            ?.let { centavos -> formatEditableMoney(centavos) }
                            .orEmpty(),
                        date = purchase.date,
                        canEditCurrentPurchase = canEdit(purchase, user),
                        editBlockedMessage = if (canEdit(purchase, user)) {
                            null
                        } else {
                            "Esta compra ya no se puede editar desde este usuario."
                        },
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onSupplierSelected(id: String) {
        formState.update {
            it.copy(
                selectedSupplierId = id,
                supplierNoteError = null,
                errorMessage = null,
            )
        }
    }

    fun onSupplierNoteChanged(value: String) {
        formState.update {
            it.copy(supplierNoteFreeform = value, supplierNoteError = null, errorMessage = null)
        }
    }

    fun onQuantityChanged(value: String) {
        formState.update {
            it.copy(quantityTons = value, quantityError = null, errorMessage = null)
        }
    }

    fun onQuantityBlurred() {
        val error = validateQuantity(formState.value.quantityTons)
        formState.update { it.copy(quantityError = error) }
    }

    fun onPriceChanged(value: String) {
        formState.update {
            it.copy(pricePerTonMxn = value, priceError = null, errorMessage = null)
        }
    }

    fun onPriceBlurred() {
        val error = validatePrice(formState.value.pricePerTonMxn)
        formState.update { it.copy(priceError = error) }
    }

    fun onDateSelected(timestamp: Timestamp) {
        formState.update { it.copy(date = timestamp, errorMessage = null) }
    }

    fun clearError() {
        formState.update { it.copy(errorMessage = null) }
    }

    fun onSave() {
        viewModelScope.launch {
            val state = uiState.value
            val user = state.user
            val quantityError = validateQuantity(state.quantityTons)
            val priceError = validatePrice(state.pricePerTonMxn)
            val supplierNoteError = validateSupplierNote(state)

            if (user == null) {
                formState.update { it.copy(errorMessage = "Inicia sesión para guardar.") }
                return@launch
            }
            if (state.isEditMode && !state.canEditCurrentPurchase) {
                formState.update {
                    it.copy(errorMessage = state.editBlockedMessage)
                }
                return@launch
            }
            if (quantityError != null || priceError != null || supplierNoteError != null) {
                formState.update {
                    it.copy(
                        quantityError = quantityError,
                        priceError = priceError,
                        supplierNoteError = supplierNoteError,
                    )
                }
                return@launch
            }

            val selectedSupplier = state.suppliers.firstOrNull {
                it.id == state.selectedSupplierId
            }
            if (selectedSupplier == null) {
                formState.update { it.copy(errorMessage = "Selecciona un proveedor.") }
                return@launch
            }

            formState.update { it.copy(isSaving = true, errorMessage = null) }

            val enteredAt = Timestamp.now()
            val original = loadedPurchase
            val purchase = Purchase(
                id = original?.id ?: UUID.randomUUID().toString(),
                supplierId = selectedSupplier.id,
                supplierName = selectedSupplier.name,
                supplierNoteFreeform = state.supplierNoteFreeform.trim()
                    .takeIf { selectedSupplier.id == Supplier.UNREGISTERED_ID && it.isNotBlank() },
                quantityTons = state.quantityTons.toDouble(),
                pricePerTonCentavos = state.pricePerTonMxn.parseMxnToCentavos(),
                date = state.date,
                dateKey = state.date.toDateKey(),
                createdBy = original?.createdBy ?: user.id,
                createdByName = original?.createdByName ?: user.displayName,
                enteredAt = if (original == null) enteredAt else original.enteredAt,
                serverWrittenAt = original?.serverWrittenAt ?: enteredAt,
                deletedAt = original?.deletedAt,
                deletedBy = original?.deletedBy,
            )

            val result = if (original == null) {
                purchaseRepository.add(purchase).map { Unit }
            } else {
                purchaseRepository.update(purchase)
            }

            formState.update {
                if (result.isSuccess) {
                    it.copy(isSaving = false, saveCompleted = true)
                } else {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "No se pudo guardar la compra.",
                    )
                }
            }
        }
    }

    fun canEdit(purchase: Purchase, user: User): Boolean {
        if (user.role == UserRole.ADMIN) return true
        if (purchase.createdBy != user.id) return false

        val writtenAt = Instant.ofEpochSecond(
            purchase.serverWrittenAt.seconds,
            purchase.serverWrittenAt.nanoseconds.toLong(),
        )
        return writtenAt.until(Instant.now(), ChronoUnit.HOURS) < 24
    }

    private fun validateQuantity(value: String): String? {
        val quantity = value.toDoubleOrNull()
        return when {
            value.isBlank() -> "Ingresa las toneladas."
            quantity == null || quantity <= 0.0 -> "Ingresa una cantidad mayor a 0."
            else -> null
        }
    }

    private fun validatePrice(value: String): String? =
        try {
            val centavos = value.parseMxnToCentavos()
            if (centavos != null && centavos <= 0L) {
                "Ingresa un precio mayor a 0."
            } else {
                null
            }
        } catch (_: IllegalArgumentException) {
            "Ingresa un precio válido."
        }

    private fun validateSupplierNote(state: AddEditPurchaseUiState): String? =
        if (state.isUnregisteredSupplier && state.supplierNoteFreeform.isBlank()) {
            "Escribe el nombre o referencia del proveedor."
        } else {
            null
        }

    private fun formatEditableDouble(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    private fun formatEditableMoney(centavos: Long): String =
        java.math.BigDecimal(centavos).movePointLeft(2).toPlainString()
}

private val MX_ZONE: ZoneId = ZoneId.of("America/Mexico_City")

private fun todayTimestamp(): Timestamp {
    val instant = LocalDate.now(MX_ZONE).atStartOfDay(MX_ZONE).toInstant()
    return Timestamp(instant.epochSecond, instant.nano)
}
