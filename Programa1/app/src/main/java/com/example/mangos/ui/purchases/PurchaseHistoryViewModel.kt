package com.example.mangos.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangos.data.model.Purchase
import com.example.mangos.data.model.Supplier
import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.PurchaseRepository
import com.example.mangos.data.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PurchaseHistoryUiState(
    val user: User? = null,
    val suppliers: List<Supplier> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val selectedSupplierId: String? = null,
    val isDeleting: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseHistoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    supplierRepository: SupplierRepository,
    private val purchaseRepository: PurchaseRepository,
) : ViewModel() {

    private val _selectedSupplierId = MutableStateFlow<String?>(null)
    private val transientState = MutableStateFlow(PurchaseHistoryTransientState())

    val selectedSupplierId: StateFlow<String?> = _selectedSupplierId

    val purchases: StateFlow<List<Purchase>> = _selectedSupplierId
        .flatMapLatest { supplierId ->
            if (supplierId == null) {
                purchaseRepository.observeRecent(HISTORY_LIMIT)
            } else {
                purchaseRepository.observeBySupplier(supplierId, HISTORY_LIMIT)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<PurchaseHistoryUiState> = combine(
        authRepository.currentUser,
        supplierRepository.observeActive(),
        _selectedSupplierId,
        purchases,
        transientState,
    ) { user, suppliers, selectedSupplierId, purchases, transient ->
        PurchaseHistoryUiState(
            user = user,
            suppliers = suppliers.sortedWith(
                compareBy<Supplier> { it.id == Supplier.UNREGISTERED_ID }
                    .thenBy { it.name.lowercase() }
            ),
            purchases = purchases,
            selectedSupplierId = selectedSupplierId,
            isDeleting = transient.isDeleting,
            message = transient.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PurchaseHistoryUiState(),
    )

    fun onSupplierFilterSelected(supplierId: String?) {
        _selectedSupplierId.value = supplierId
    }

    fun clearMessage() {
        transientState.update { it.copy(message = null) }
    }

    fun softDelete(purchase: Purchase) {
        viewModelScope.launch {
            val user = uiState.value.user
            if (user == null) {
                transientState.update { it.copy(message = "Inicia sesion para eliminar.") }
                return@launch
            }
            if (!canEdit(purchase, user)) {
                transientState.update {
                    it.copy(message = "Esta compra ya no se puede eliminar desde este usuario.")
                }
                return@launch
            }

            transientState.update { it.copy(isDeleting = true, message = null) }
            val result = purchaseRepository.softDelete(purchase.id, user.id)
            transientState.update {
                it.copy(
                    isDeleting = false,
                    message = if (result.isSuccess) {
                        "Compra eliminada."
                    } else {
                        result.exceptionOrNull()?.message ?: "No se pudo eliminar la compra."
                    },
                )
            }
        }
    }

    fun canEdit(purchase: Purchase, user: User?): Boolean {
        if (user == null) return false
        if (user.role == UserRole.ADMIN) return true
        if (purchase.createdBy != user.id) return false

        val writtenAt = Instant.ofEpochSecond(
            purchase.serverWrittenAt.seconds,
            purchase.serverWrittenAt.nanoseconds.toLong(),
        )
        return writtenAt.until(Instant.now(), ChronoUnit.HOURS) < 24
    }

    companion object {
        private const val HISTORY_LIMIT = 50
    }
}

private data class PurchaseHistoryTransientState(
    val isDeleting: Boolean = false,
    val message: String? = null,
)
