package com.example.mangos.data.repository.functions

import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.UserAdminRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class UserAdminRepositoryFunctionsImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val sparkFallback: UserAdminRepositorySparkFallback,
) : UserAdminRepository {

    override fun observeOperators(): Flow<List<User>> =
        firestore.collection(USERS)
            .whereEqualTo("role", OPERATOR_ROLE_VALUE)
            .snapshots()
            .map { snap ->
                snap.documents
                    .mapNotNull { it.toOperator() }
                    .filterActiveOperators()
            }
            .catch { emit(emptyList()) }

    override suspend fun refreshOperators(): Result<Unit> = runCatching {
        try {
            fetchOperators()
        } catch (error: FirebaseFirestoreException) {
            throw IllegalStateException(firestoreErrorMessage(error), error)
        }
        Unit
    }

    override suspend fun createOperator(
        email: String,
        displayName: String,
        password: String,
    ): Result<Unit> {
        val normalizedEmail = email.trim()
        val normalizedDisplayName = displayName.trim()
        return runPrivilegedMutation(
            callableName = "createOperatorAccount",
            payload = mapOf(
                "email" to normalizedEmail,
                "displayName" to normalizedDisplayName,
                "password" to password,
            ),
            adminPasswordConfirmation = null,
            fallback = {
                sparkFallback.createAccount(
                    email = normalizedEmail,
                    displayName = normalizedDisplayName,
                    password = password,
                    role = OPERATOR_ROLE_VALUE,
                )
            },
        )
    }

    override suspend fun createAdmin(
        email: String,
        displayName: String,
        password: String,
        adminPasswordConfirmation: String,
    ): Result<Unit> {
        val normalizedEmail = email.trim()
        val normalizedDisplayName = displayName.trim()
        return runPrivilegedMutation(
            callableName = "createAdminAccount",
            payload = mapOf(
                "email" to normalizedEmail,
                "displayName" to normalizedDisplayName,
                "password" to password,
                "adminPasswordConfirmation" to adminPasswordConfirmation,
            ),
            adminPasswordConfirmation = adminPasswordConfirmation,
            fallback = {
                sparkFallback.createAccount(
                    email = normalizedEmail,
                    displayName = normalizedDisplayName,
                    password = password,
                    role = ADMIN_ROLE_VALUE,
                )
            },
        )
    }

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
        fallback = {
            sparkFallback.promoteOperatorToAdmin(
                operatorUserId = operatorUserId,
                operatorPasswordConfirmation = operatorPasswordConfirmation,
            )
        },
    )

    private suspend fun runPrivilegedMutation(
        callableName: String,
        payload: Map<String, Any?>,
        adminPasswordConfirmation: String?,
        fallback: suspend () -> Unit,
    ): Result<Unit> = runCatching {
        if (adminPasswordConfirmation != null) {
            reauthenticateCurrentUser(adminPasswordConfirmation)
        }
        try {
            callFunction(callableName, payload)
        } catch (error: FirebaseFunctionsException) {
            if (error.code == FirebaseFunctionsException.Code.NOT_FOUND) {
                fallback()
            } else {
                throw IllegalStateException(functionsErrorMessage(error), error)
            }
        }
        refreshOperators().getOrThrow()
    }

    private suspend fun fetchOperators(): List<User> {
        val snap = firestore.collection(USERS)
            .whereEqualTo("role", OPERATOR_ROLE_VALUE)
            .get()
            .await()

        return snap.documents
            .mapNotNull { it.toOperator() }
            .filterActiveOperators()
    }

    private suspend fun callFunction(callableName: String, payload: Map<String, Any?>) {
        functions
            .getHttpsCallable(callableName)
            .call(payload)
            .await()
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

    private fun DocumentSnapshot.toOperator(): User? {
        if (!exists() || getString("role") != OPERATOR_ROLE_VALUE) return null
        return User(
            id = id,
            email = getString("email").orEmpty(),
            displayName = getString("displayName").orEmpty(),
            role = UserRole.OPERATOR,
            accountCreatedAt = getTimestamp("accountCreatedAt") ?: Timestamp.now(),
            disabledAt = getTimestamp("disabledAt"),
            retiredAt = getTimestamp("retiredAt"),
            promotedToUid = getString("promotedToUid"),
        )
    }

    private fun List<User>.filterActiveOperators(): List<User> =
        filter { it.disabledAt == null && it.retiredAt == null }

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

    private fun functionsErrorMessage(error: FirebaseFunctionsException): String {
        val serverMessage = error.message.orEmpty()
        if (serverMessage.isNotBlank() && !serverMessage.equals(error.code.name, ignoreCase = true)) {
            return serverMessage
        }

        return when (error.code) {
            FirebaseFunctionsException.Code.NOT_FOUND ->
                "No se encontro la funcion de administracion de usuarios en Firebase. Despliega Cloud Functions para este proyecto."
            FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                "Inicia sesion para continuar."
            FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                "Solo administradores pueden administrar usuarios."
            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                "Sin conexion con Firebase. Verifica tu red e intentalo de nuevo."
            else -> "No se pudo completar la operacion de usuarios."
        }
    }

    private fun firestoreErrorMessage(error: FirebaseFirestoreException): String =
        when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "No tienes permiso para leer usuarios. Verifica que esta cuenta sea administradora y que las reglas de Firestore esten desplegadas."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Sin conexion con Firestore. Verifica tu red e intentalo de nuevo."
            else -> error.localizedMessage ?: "No se pudo leer la lista de usuarios."
        }

    private companion object {
        const val USERS = "users"
        const val ADMIN_ROLE_VALUE = "admin"
        const val OPERATOR_ROLE_VALUE = "operator"
    }
}
