package com.example.mangos.ui.users

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mangos.data.model.User

@Composable
fun UserManagementScreen(
    viewModel: UserManagementViewModel = hiltViewModel(),
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

    UserManagementContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        onShowRoster = viewModel::showRoster,
        onShowCreateOperator = viewModel::showCreateOperator,
        onShowCreateAdmin = viewModel::showCreateAdmin,
        onPromoteClick = viewModel::showPromote,
        onEmailChanged = viewModel::onEmailChanged,
        onDisplayNameChanged = viewModel::onDisplayNameChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onOperatorPasswordConfirmationChanged = viewModel::onOperatorPasswordConfirmationChanged,
        onAdminEmailConfirmationChanged = viewModel::onAdminEmailConfirmationChanged,
        onAdminPasswordConfirmationChanged = viewModel::onAdminPasswordConfirmationChanged,
        onSubmitCreateOperator = viewModel::submitCreateOperator,
        onSubmitCreateAdmin = viewModel::submitCreateAdmin,
        onSubmitPromotion = viewModel::submitPromotion,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserManagementContent(
    uiState: UserManagementUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onShowRoster: () -> Unit,
    onShowCreateOperator: () -> Unit,
    onShowCreateAdmin: () -> Unit,
    onPromoteClick: (User) -> Unit,
    onEmailChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onOperatorPasswordConfirmationChanged: (String) -> Unit,
    onAdminEmailConfirmationChanged: (String) -> Unit,
    onAdminPasswordConfirmationChanged: (String) -> Unit,
    onSubmitCreateOperator: () -> Unit,
    onSubmitCreateAdmin: () -> Unit,
    onSubmitPromotion: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.mode) {
                            UserManagementMode.ROSTER -> "Usuarios"
                            UserManagementMode.CREATE_OPERATOR -> "Nuevo operador"
                            UserManagementMode.CREATE_ADMIN -> "Nuevo admin"
                            UserManagementMode.PROMOTE_OPERATOR -> "Promover operador"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (uiState.mode != UserManagementMode.ROSTER) {
                        IconButton(onClick = onShowRoster) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshing && !uiState.isSubmitting,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Actualizar",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (!uiState.canManageUsers) {
            AdminOnlyMessage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            )
            return@Scaffold
        }

        when (uiState.mode) {
            UserManagementMode.ROSTER -> OperatorRoster(
                uiState = uiState,
                contentPadding = innerPadding,
                onShowCreateOperator = onShowCreateOperator,
                onShowCreateAdmin = onShowCreateAdmin,
                onPromoteClick = onPromoteClick,
            )
            UserManagementMode.CREATE_OPERATOR -> CreateAccountForm(
                uiState = uiState,
                contentPadding = innerPadding,
                isAdmin = false,
                onEmailChanged = onEmailChanged,
                onDisplayNameChanged = onDisplayNameChanged,
                onPasswordChanged = onPasswordChanged,
                onAdminEmailConfirmationChanged = onAdminEmailConfirmationChanged,
                onAdminPasswordConfirmationChanged = onAdminPasswordConfirmationChanged,
                onSubmit = onSubmitCreateOperator,
            )
            UserManagementMode.CREATE_ADMIN -> CreateAccountForm(
                uiState = uiState,
                contentPadding = innerPadding,
                isAdmin = true,
                onEmailChanged = onEmailChanged,
                onDisplayNameChanged = onDisplayNameChanged,
                onPasswordChanged = onPasswordChanged,
                onAdminEmailConfirmationChanged = onAdminEmailConfirmationChanged,
                onAdminPasswordConfirmationChanged = onAdminPasswordConfirmationChanged,
                onSubmit = onSubmitCreateAdmin,
            )
            UserManagementMode.PROMOTE_OPERATOR -> PromoteOperatorForm(
                uiState = uiState,
                contentPadding = innerPadding,
                onOperatorPasswordConfirmationChanged = onOperatorPasswordConfirmationChanged,
                onAdminEmailConfirmationChanged = onAdminEmailConfirmationChanged,
                onAdminPasswordConfirmationChanged = onAdminPasswordConfirmationChanged,
                onSubmit = onSubmitPromotion,
            )
        }
    }
}

@Composable
private fun OperatorRoster(
    uiState: UserManagementUiState,
    contentPadding: PaddingValues,
    onShowCreateOperator: () -> Unit,
    onShowCreateAdmin: () -> Unit,
    onPromoteClick: (User) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onShowCreateOperator,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canSubmit,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Operador")
                }
                FilledTonalButton(
                    onClick = onShowCreateAdmin,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canSubmit,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin")
                }
            }
        }

        item {
            Text(
                text = "Operadores",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (uiState.operators.isEmpty()) {
            item {
                EmptyOperators()
            }
        } else {
            items(
                items = uiState.operators,
                key = { it.id },
            ) { operator ->
                OperatorCard(
                    operator = operator,
                    onPromoteClick = { onPromoteClick(operator) },
                    enabled = uiState.canSubmit,
                )
            }
        }
    }
}

@Composable
private fun OperatorCard(
    operator: User,
    onPromoteClick: () -> Unit,
    enabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = operator.displayName.ifBlank { operator.email },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = operator.email,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                IconButton(
                    onClick = onPromoteClick,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SupervisorAccount,
                        contentDescription = "Promover a admin",
                    )
                }
            },
        )
    }
}

@Composable
private fun CreateAccountForm(
    uiState: UserManagementUiState,
    contentPadding: PaddingValues,
    isAdmin: Boolean,
    onEmailChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onAdminEmailConfirmationChanged: (String) -> Unit,
    onAdminPasswordConfirmationChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit,
                label = { Text("Nombre") },
                singleLine = true,
                isError = uiState.displayNameError != null,
                supportingText = uiState.displayNameError?.let { { Text(it) } },
            )
        }
        item {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit,
                label = { Text("Correo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { { Text(it) } },
            )
        }
        item {
            PasswordField(
                value = uiState.password,
                onValueChange = onPasswordChanged,
                enabled = uiState.canSubmit,
                label = "Contrasena inicial",
                error = uiState.passwordError,
            )
        }
        if (isAdmin) {
            item {
                AdminConfirmationFields(
                    uiState = uiState,
                    onAdminEmailConfirmationChanged = onAdminEmailConfirmationChanged,
                    onAdminPasswordConfirmationChanged = onAdminPasswordConfirmationChanged,
                )
            }
        }
        item {
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit,
            ) {
                Icon(
                    imageVector = if (isAdmin) {
                        Icons.Filled.AdminPanelSettings
                    } else {
                        Icons.Filled.PersonAdd
                    },
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (uiState.isSubmitting) {
                        "Creando..."
                    } else if (isAdmin) {
                        "Crear admin"
                    } else {
                        "Crear operador"
                    },
                )
            }
        }
    }
}

@Composable
private fun PromoteOperatorForm(
    uiState: UserManagementUiState,
    contentPadding: PaddingValues,
    onOperatorPasswordConfirmationChanged: (String) -> Unit,
    onAdminEmailConfirmationChanged: (String) -> Unit,
    onAdminPasswordConfirmationChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SelectedOperatorSummary(operator = uiState.selectedOperator)
        }
        item {
            PasswordField(
                value = uiState.operatorPasswordConfirmation,
                onValueChange = onOperatorPasswordConfirmationChanged,
                enabled = uiState.canSubmit,
                label = "Contrasena del operador",
                error = uiState.operatorPasswordError,
            )
        }
        item {
            AdminConfirmationFields(
                uiState = uiState,
                onAdminEmailConfirmationChanged = onAdminEmailConfirmationChanged,
                onAdminPasswordConfirmationChanged = onAdminPasswordConfirmationChanged,
            )
        }
        item {
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit,
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isSubmitting) "Promoviendo..." else "Promover a admin")
            }
        }
    }
}

@Composable
private fun AdminConfirmationFields(
    uiState: UserManagementUiState,
    onAdminEmailConfirmationChanged: (String) -> Unit,
    onAdminPasswordConfirmationChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = uiState.adminEmailConfirmation,
            onValueChange = onAdminEmailConfirmationChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canSubmit,
            label = { Text("Correo admin") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = uiState.adminEmailError != null,
            supportingText = uiState.adminEmailError?.let { { Text(it) } },
        )
        PasswordField(
            value = uiState.adminPasswordConfirmation,
            onValueChange = onAdminPasswordConfirmationChanged,
            enabled = uiState.canSubmit,
            label = "Contrasena admin",
            error = uiState.adminPasswordError,
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    label: String,
    error: String?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
    )
}

@Composable
private fun SelectedOperatorSummary(operator: User?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = operator?.displayName?.ifBlank { operator.email } ?: "Operador",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = operator?.email.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AssistChip(
                onClick = {},
                label = { Text("operador") },
            )
        }
    }
}

@Composable
private fun EmptyOperators() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = "Sin operadores registrados",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AdminOnlyMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Solo administradores",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No tienes permiso para administrar usuarios.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
