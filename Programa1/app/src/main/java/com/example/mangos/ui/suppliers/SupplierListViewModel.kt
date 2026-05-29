package com.example.mangos.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangos.data.model.Supplier
import com.example.mangos.data.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupplierListUiState(
    val suppliers: List<Supplier> = emptyList(),
    val isDeactivating: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SupplierListViewModel @Inject constructor(
    private val supplierRepository: SupplierRepository,
) : ViewModel() {

    private val transientState = MutableStateFlow(SupplierListTransientState())

    val uiState: StateFlow<SupplierListUiState> = combine(
        supplierRepository.observeAll(),
        transientState,
    ) { suppliers, transient ->
        SupplierListUiState(
            suppliers = suppliers
                .filter { it.id != Supplier.UNREGISTERED_ID }
                .sortedWith(
                    compareByDescending<Supplier> { it.isActive }
                        .thenBy { it.name.lowercase() },
                ),
            isDeactivating = transient.isDeactivating,
            message = transient.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SupplierListUiState(),
    )

    fun deactivate(supplier: Supplier) {
        if (!supplier.isActive) {
            transientState.update { it.copy(message = "El proveedor ya esta inactivo.") }
            return
        }

        viewModelScope.launch {
            transientState.update { it.copy(isDeactivating = true, message = null) }
            val result = supplierRepository.deactivate(supplier.id)
            transientState.update {
                it.copy(
                    isDeactivating = false,
                    message = if (result.isSuccess) {
                        "Proveedor desactivado."
                    } else {
                        result.exceptionOrNull()?.message ?: "No se pudo desactivar el proveedor."
                    },
                )
            }
        }
    }

    fun clearMessage() {
        transientState.update { it.copy(message = null) }
    }
}

private data class SupplierListTransientState(
    val isDeactivating: Boolean = false,
    val message: String? = null,
)
