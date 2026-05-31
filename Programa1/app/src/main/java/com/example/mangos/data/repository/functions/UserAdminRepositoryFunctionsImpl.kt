package com.example.mangos.data.repository.functions

import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.UserAdminRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.functions.FirebaseFunctions
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

@Singleton
class UserAdminRepositoryFunctionsImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : UserAdminRepository {

    private val operators = MutableStateFlow<List<User>>(emptyList())

    override fun observeOperators(): Flow<List<User>> = operators.asStateFlow()

    override suspend fun refreshOperators(): Result<Unit> = runCatching {
        operators.value = fetchOperators()
    }

    override suspend fun createOperator(
        email: String,
        displayName: String,
        password: String,
    ): Result<Unit> = runPrivilegedMutation(
        callableName = "createOperatorAccount",
        payload = mapOf(
            "email" to email.trim(),
            "displayName" to displayName.trim(),
            "password" to password,
        ),
        adminPasswordConfirmation = null,
    )

    override suspend fun createAdmin(
        email: String,
        displayName: String,
        password: String,
        adminPasswordConfirmation: String,
    ): Result<Unit> = runPrivilegedMutation(
        callableName = "createAdminAccount",
        payload = mapOf(
            "email" to email.trim(),
            "displayName" to displayName.trim(),
            "password" to password,
            "adminPasswordConfirmation" to adminPasswordConfirmation,
        ),
        adminPasswordConfirmation = adminPasswordConfirmation,
    )

    override suspend fun promoteOperatorToAdmin(
        operatorUserId: String,
        operatorPasswordConfirmation: String,
        adminPasswordConfirmation: String,
    ): Result<Unit> = runPrivilegedMutation(
        callableName = "promoteOperatorToAdmin",
        payload = mapOf(
            "operatorUserId" to operatorUserId,
            "operatorPasswordConfirmation" to operatorPasswordConfirmation,
            "adminPasswordConfirmation" to adminPasswordConfirmation,
        ),
        adminPasswordConfirmation = adminPasswordConfirmation,
    )

    private suspend fun runPrivilegedMutation(
        callableName: String,
        payload: Map<String, Any?>,
        adminPasswordConfirmation: String?,
    ): Result<Unit> = runCatching {
        if (adminPasswordConfirmation != null) {
            reauthenticateCurrentUser(adminPasswordConfirmation)
        }
        functions
            .getHttpsCallable(callableName)
            .call(payload)
            .await()
        refreshOperators().getOrThrow()
    }

    private suspend fun fetchOperators(): List<User> {
        val data = functions
            .getHttpsCallable("listOperators")
            .call()
            .await()
            .data as? Map<*, *>
            ?: return emptyList()

        val rawOperators = data["operators"] as? List<*> ?: return emptyList()
        return rawOperators.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            User(
                id = item["id"] as? String ?: return@mapNotNull null,
                email = item["email"] as? String ?: "",
                displayName = item["displayName"] as? String ?: "",
                role = UserRole.OPERATOR,
                accountCreatedAt = millisTimestamp(item["accountCreatedAtMillis"]),
                disabledAt = millisTimestampOrNull(item["disabledAtMillis"]),
                retiredAt = millisTimestampOrNull(item["retiredAtMillis"]),
                promotedToUid = item["promotedToUid"] as? String,
            )
        }
    }

    private suspend fun reauthenticateCurrentUser(password: String) {
        val user = auth.currentUser ?: error("No hay sesion activa.")
        val email = user.email ?: error("La cuenta actual no tiene correo.")
        val credential = EmailAuthProvider.getCredential(email, password)
        try {
            user.reauthenticate(credential).await()
        } catch (error: FirebaseAuthException) {
            throw IllegalStateException(authErrorMessage(error), error)
        }
    }

    private fun millisTimestamp(raw: Any?): Timestamp =
        millisTimestampOrNull(raw) ?: Timestamp.now()

    private fun millisTimestampOrNull(raw: Any?): Timestamp? {
        val millis = when (raw) {
            is Long -> raw
            is Int -> raw.toLong()
            is Double -> raw.toLong()
            else -> return null
        }
        return Timestamp(Date(millis))
    }

    private fun authErrorMessage(error: FirebaseAuthException): String = when (error.errorCode) {
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_LOGIN_CREDENTIALS" ->
            "Las credenciales del administrador no coinciden."
        "ERROR_TOO_MANY_REQUESTS" ->
            "Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexion. Verifica tu red e intentalo de nuevo."
        else -> error.localizedMessage ?: "No se pudo confirmar la credencial."
    }
}
