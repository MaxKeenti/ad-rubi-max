package com.example.mangos.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.UserAdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UserManagementMode {
    ROSTER,
    CREATE_OPERATOR,
    CREATE_ADMIN,
    PROMOTE_OPERATOR,
}

data class UserManagementUiState(
    val currentUser: User? = null,
    val operators: List<User> = emptyList(),
    val mode: UserManagementMode = UserManagementMode.ROSTER,
    val selectedOperator: User? = null,
    val email: String = "",
    val displayName: String = "",
    val password: String = "",
    val operatorPasswordConfirmation: String = "",
    val adminEmailConfirmation: String = "",
    val adminPasswordConfirmation: String = "",
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val emailError: String? = null,
    val displayNameError: String? = null,
    val passwordError: String? = null,
    val operatorPasswordError: String? = null,
    val adminEmailError: String? = null,
    val adminPasswordError: String? = null,
) {
    val canManageUsers: Boolean = currentUser?.role == UserRole.ADMIN
    val canSubmit: Boolean = canManageUsers && !isSubmitting && !isRefreshing
}

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userAdminRepository: UserAdminRepository,
) : ViewModel() {

    private val formState = MutableStateFlow(UserManagementFormState())

    val uiState: StateFlow<UserManagementUiState> = combine(
        authRepository.currentUser,
        userAdminRepository.observeOperators(),
        formState,
    ) { user, operators, form ->
        UserManagementUiState(
            currentUser = user,
            operators = operators.sortedWith(compareBy<User> { it.displayName.lowercase() }
                .thenBy { it.email.lowercase() }),
            mode = form.mode,
            selectedOperator = form.selectedOperator,
            email = form.email,
            displayName = form.displayName,
            password = form.password,
            operatorPasswordConfirmation = form.operatorPasswordConfirmation,
            adminEmailConfirmation = form.adminEmailConfirmation,
            adminPasswordConfirmation = form.adminPasswordConfirmation,
            isRefreshing = form.isRefreshing,
            isSubmitting = form.isSubmitting,
            message = form.message,
            emailError = form.emailError,
            displayNameError = form.displayNameError,
            passwordError = form.passwordError,
            operatorPasswordError = form.operatorPasswordError,
            adminEmailError = form.adminEmailError,
            adminPasswordError = form.adminPasswordError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserManagementUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            formState.update { it.copy(isRefreshing = true, message = null) }
            val result = userAdminRepository.refreshOperators()
            formState.update {
                it.copy(
                    isRefreshing = false,
                    message = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun showRoster() {
        formState.update {
            UserManagementFormState(
                mode = UserManagementMode.ROSTER,
                message = it.message,
                isRefreshing = it.isRefreshing,
            )
        }
    }

    fun showCreateOperator() {
        formState.update {
            UserManagementFormState(
                mode = UserManagementMode.CREATE_OPERATOR,
                message = it.message,
                isRefreshing = it.isRefreshing,
            )
        }
    }

    fun showCreateAdmin() {
        formState.update {
            UserManagementFormState(
                mode = UserManagementMode.CREATE_ADMIN,
                adminEmailConfirmation = uiState.value.currentUser?.email.orEmpty(),
                message = it.message,
                isRefreshing = it.isRefreshing,
            )
        }
    }

    fun showPromote(operator: User) {
        formState.update {
            UserManagementFormState(
                mode = UserManagementMode.PROMOTE_OPERATOR,
                selectedOperator = operator,
                adminEmailConfirmation = uiState.value.currentUser?.email.orEmpty(),
                message = it.message,
                isRefreshing = it.isRefreshing,
            )
        }
    }

    fun onEmailChanged(value: String) {
        formState.update { it.copy(email = value, emailError = null, message = null) }
    }

    fun onDisplayNameChanged(value: String) {
        formState.update { it.copy(displayName = value, displayNameError = null, message = null) }
    }

    fun onPasswordChanged(value: String) {
        formState.update { it.copy(password = value, passwordError = null, message = null) }
    }

    fun onOperatorPasswordConfirmationChanged(value: String) {
        formState.update {
            it.copy(operatorPasswordConfirmation = value, operatorPasswordError = null, message = null)
        }
    }

    fun onAdminEmailConfirmationChanged(value: String) {
        formState.update { it.copy(adminEmailConfirmation = value, adminEmailError = null, message = null) }
    }

    fun onAdminPasswordConfirmationChanged(value: String) {
        formState.update {
            it.copy(adminPasswordConfirmation = value, adminPasswordError = null, message = null)
        }
    }

    fun submitCreateOperator() {
        submitCreateAccount(isAdmin = false)
    }

    fun submitCreateAdmin() {
        submitCreateAccount(isAdmin = true)
    }

    fun submitPromotion() {
        viewModelScope.launch {
            val state = uiState.value
            if (!state.canManageUsers) {
                formState.update { it.copy(message = "Solo administradores pueden administrar usuarios.") }
                return@launch
            }

            val operator = state.selectedOperator
            val operatorPasswordError = validatePassword(state.operatorPasswordConfirmation)
            val adminEmailError = validateAdminEmail(state.adminEmailConfirmation, state.currentUser?.email)
            val adminPasswordError = validatePassword(state.adminPasswordConfirmation)

            if (operator == null || operatorPasswordError != null ||
                adminEmailError != null || adminPasswordError != null
            ) {
                formState.update {
                    it.copy(
                        operatorPasswordError = operatorPasswordError,
                        adminEmailError = adminEmailError,
                        adminPasswordError = adminPasswordError,
                        message = if (operator == null) "Selecciona un operador." else null,
                    )
                }
                return@launch
            }

            formState.update { it.copy(isSubmitting = true, message = null) }
            val result = userAdminRepository.promoteOperatorToAdmin(
                operatorUserId = operator.id,
                operatorPasswordConfirmation = state.operatorPasswordConfirmation,
                adminPasswordConfirmation = state.adminPasswordConfirmation,
            )
            formState.update {
                if (result.isSuccess) {
                    UserManagementFormState(
                        mode = UserManagementMode.ROSTER,
                        message = "Operador promovido a administrador.",
                    )
                } else {
                    it.copy(
                        isSubmitting = false,
                        message = result.exceptionOrNull()?.message
                            ?: "No se pudo promover el operador.",
                    )
                }
            }
        }
    }

    fun clearMessage() {
        formState.update { it.copy(message = null) }
    }

    private fun submitCreateAccount(isAdmin: Boolean) {
        viewModelScope.launch {
            val state = uiState.value
            if (!state.canManageUsers) {
                formState.update { it.copy(message = "Solo administradores pueden administrar usuarios.") }
                return@launch
            }

            val emailError = validateEmail(state.email)
            val displayNameError = validateRequired(state.displayName, "Ingresa el nombre.")
            val passwordError = validatePassword(state.password)
            val adminEmailError = if (isAdmin) {
                validateAdminEmail(state.adminEmailConfirmation, state.currentUser?.email)
            } else {
                null
            }
            val adminPasswordError = if (isAdmin) {
                validatePassword(state.adminPasswordConfirmation)
            } else {
                null
            }

            if (emailError != null || displayNameError != null || passwordError != null ||
                adminEmailError != null || adminPasswordError != null
            ) {
                formState.update {
                    it.copy(
                        emailError = emailError,
                        displayNameError = displayNameError,
                        passwordError = passwordError,
                        adminEmailError = adminEmailError,
                        adminPasswordError = adminPasswordError,
                    )
                }
                return@launch
            }

            formState.update { it.copy(isSubmitting = true, message = null) }
            val result = if (isAdmin) {
                userAdminRepository.createAdmin(
                    email = state.email,
                    displayName = state.displayName,
                    password = state.password,
                    adminPasswordConfirmation = state.adminPasswordConfirmation,
                )
            } else {
                userAdminRepository.createOperator(
                    email = state.email,
                    displayName = state.displayName,
                    password = state.password,
                )
            }

            formState.update {
                if (result.isSuccess) {
                    UserManagementFormState(
                        mode = UserManagementMode.ROSTER,
                        message = if (isAdmin) {
                            "Administrador creado."
                        } else {
                            "Operador creado."
                        },
                    )
                } else {
                    it.copy(
                        isSubmitting = false,
                        message = result.exceptionOrNull()?.message
                            ?: "No se pudo crear la cuenta.",
                    )
                }
            }
        }
    }

    private fun validateEmail(value: String): String? {
        val trimmed = value.trim()
        val atIndex = trimmed.indexOf('@')
        val dotIndex = trimmed.lastIndexOf('.')
        return if (atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < trimmed.lastIndex) {
            null
        } else {
            "Ingresa un correo valido."
        }
    }

    private fun validateAdminEmail(value: String, currentEmail: String?): String? {
        val expected = currentEmail.orEmpty().trim()
        return if (value.trim().equals(expected, ignoreCase = true) && expected.isNotBlank()) {
            null
        } else {
            "Confirma el correo del administrador."
        }
    }

    private fun validatePassword(value: String): String? =
        if (value.length >= 6) null else "La contrasena debe tener al menos 6 caracteres."

    private fun validateRequired(value: String, message: String): String? =
        if (value.isBlank()) message else null
}

private data class UserManagementFormState(
    val mode: UserManagementMode = UserManagementMode.ROSTER,
    val selectedOperator: User? = null,
    val email: String = "",
    val displayName: String = "",
    val password: String = "",
    val operatorPasswordConfirmation: String = "",
    val adminEmailConfirmation: String = "",
    val adminPasswordConfirmation: String = "",
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val emailError: String? = null,
    val displayNameError: String? = null,
    val passwordError: String? = null,
    val operatorPasswordError: String? = null,
    val adminEmailError: String? = null,
    val adminPasswordError: String? = null,
)
