package com.example.mangos.ui.suppliers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangos.data.model.Supplier
import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.SupplierRepository
import com.example.mangos.ui.navigation.Screen
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditSupplierUiState(
    val supplierId: String? = null,
    val user: User? = null,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val location: String = "",
    val mangoVariety: String = "",
    val isActive: Boolean = true,
    val nameError: String? = null,
    val emailError: String? = null,
    val locationError: String? = null,
    val mangoVarietyError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEditMode: Boolean = supplierId != null
    val canSave: Boolean = user?.role == UserRole.ADMIN && !isLoading && !isSaving
}

@HiltViewModel
class AddEditSupplierViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val supplierRepository: SupplierRepository,
) : ViewModel() {

    private val supplierId: String? =
        savedStateHandle[Screen.AddEditSupplier.ARG_SUPPLIER_ID]

    private val formState = MutableStateFlow(
        AddEditSupplierUiState(
            supplierId = supplierId,
            isLoading = supplierId != null,
        )
    )

    private var loadedSupplier: Supplier? = null

    val uiState: StateFlow<AddEditSupplierUiState> = combine(
        formState,
        authRepository.currentUser,
    ) { form, user ->
        form.copy(user = user)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = formState.value,
    )

    init {
        if (supplierId != null) {
            viewModelScope.launch {
                val supplier = supplierRepository.getById(supplierId)
                loadedSupplier = supplier
                formState.update {
                    if (supplier == null) {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se encontro el proveedor.",
                        )
                    } else {
                        it.copy(
                            name = supplier.name,
                            phone = supplier.phone,
                            email = supplier.email,
                            location = supplier.location,
                            mangoVariety = supplier.mangoVariety,
                            isActive = supplier.isActive,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        formState.update { it.copy(name = value, nameError = null, errorMessage = null) }
    }

    fun onPhoneChanged(value: String) {
        formState.update { it.copy(phone = value, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        formState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun onLocationChanged(value: String) {
        formState.update { it.copy(location = value, locationError = null, errorMessage = null) }
    }

    fun onMangoVarietyChanged(value: String) {
        formState.update {
            it.copy(mangoVariety = value, mangoVarietyError = null, errorMessage = null)
        }
    }

    fun onActiveChanged(value: Boolean) {
        formState.update { it.copy(isActive = value, errorMessage = null) }
    }

    fun onNameBlurred() {
        formState.update { it.copy(nameError = validateRequired(it.name, "Ingresa el nombre.")) }
    }

    fun onEmailBlurred() {
        formState.update { it.copy(emailError = validateEmail(it.email)) }
    }

    fun onLocationBlurred() {
        formState.update {
            it.copy(locationError = validateRequired(it.location, "Ingresa la ubicacion."))
        }
    }

    fun onMangoVarietyBlurred() {
        formState.update {
            it.copy(
                mangoVarietyError = validateRequired(
                    value = it.mangoVariety,
                    message = "Ingresa la variedad de mango.",
                ),
            )
        }
    }

    fun clearError() {
        formState.update { it.copy(errorMessage = null) }
    }

    fun onSave() {
        viewModelScope.launch {
            val state = uiState.value
            val user = state.user
            val nameError = validateRequired(state.name, "Ingresa el nombre.")
            val emailError = validateEmail(state.email)
            val locationError = validateRequired(state.location, "Ingresa la ubicacion.")
            val mangoVarietyError = validateRequired(
                value = state.mangoVariety,
                message = "Ingresa la variedad de mango.",
            )

            if (user == null) {
                formState.update { it.copy(errorMessage = "Inicia sesion para guardar.") }
                return@launch
            }
            if (user.role != UserRole.ADMIN) {
                formState.update { it.copy(errorMessage = "Solo administradores pueden guardar proveedores.") }
                return@launch
            }
            if (nameError != null || emailError != null ||
                locationError != null || mangoVarietyError != null
            ) {
                formState.update {
                    it.copy(
                        nameError = nameError,
                        emailError = emailError,
                        locationError = locationError,
                        mangoVarietyError = mangoVarietyError,
                    )
                }
                return@launch
            }

            formState.update { it.copy(isSaving = true, errorMessage = null) }

            val original = loadedSupplier
            val supplier = Supplier(
                id = original?.id ?: UUID.randomUUID().toString(),
                name = state.name.trim(),
                phone = state.phone.trim(),
                email = state.email.trim(),
                location = state.location.trim(),
                mangoVariety = state.mangoVariety.trim(),
                isActive = state.isActive,
                createdAt = original?.createdAt ?: Timestamp.now(),
                createdBy = original?.createdBy ?: user.id,
            )

            val result = if (original == null) {
                supplierRepository.add(supplier).map { Unit }
            } else {
                supplierRepository.update(supplier)
            }

            formState.update {
                if (result.isSuccess) {
                    it.copy(isSaving = false, saveCompleted = true)
                } else {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "No se pudo guardar el proveedor.",
                    )
                }
            }
        }
    }

    private fun validateRequired(value: String, message: String): String? =
        if (value.isBlank()) message else null

    private fun validateEmail(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        val atIndex = trimmed.indexOf('@')
        val dotIndex = trimmed.lastIndexOf('.')
        return if (atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < trimmed.lastIndex) {
            null
        } else {
            "Ingresa un correo valido."
        }
    }
}
