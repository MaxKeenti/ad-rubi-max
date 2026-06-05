package com.example.mangos.data.repository.functions

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.app
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Singleton
class UserAdminRepositorySparkFallback @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun createAccount(
        email: String,
        displayName: String,
        password: String,
        role: String,
    ) {
        val normalizedEmail = email.trim().lowercase()
        val normalizedDisplayName = displayName.trim()
        val account = signUp(normalizedEmail, password)
        try {
            firestore.collection(USERS).document(account.localId)
                .set(
                    mapOf(
                        "email" to normalizedEmail,
                        "displayName" to normalizedDisplayName,
                        "role" to role,
                        "accountCreatedAt" to FieldValue.serverTimestamp(),
                        "disabledAt" to null,
                        "retiredAt" to null,
                    ),
                )
                .await()
        } catch (error: Exception) {
            deleteAccountBestEffort(account.idToken)
            throw IllegalStateException(firestoreWriteErrorMessage(error), error)
        }
    }

    suspend fun promoteOperatorToAdmin(
        operatorUserId: String,
        operatorPasswordConfirmation: String,
    ) {
        val operatorRef = firestore.collection(USERS).document(operatorUserId)
        val operatorSnap = operatorRef.get().await()
        if (!operatorSnap.exists()) {
            error("No se encontro el operador.")
        }

        if (operatorSnap.getString("role") != OPERATOR_ROLE_VALUE) {
            error("El usuario seleccionado no es operador.")
        }
        if (operatorSnap.getTimestamp("disabledAt") != null || operatorSnap.getTimestamp("retiredAt") != null) {
            error("El operador ya esta retirado.")
        }

        val email = operatorSnap.getString("email")?.trim()?.lowercase().orEmpty()
        if (email.isBlank()) {
            error("El operador no tiene correo registrado.")
        }
        val displayName = operatorSnap.getString("displayName")?.trim().orEmpty().ifBlank { email }
        val operatorSession = signIn(
            email = email,
            password = operatorPasswordConfirmation,
            credentialErrorMessage = "La contrasena del operador no coincide.",
        )
        if (operatorSession.localId != operatorUserId) {
            error("La credencial no pertenece al operador seleccionado.")
        }

        deleteAccount(operatorSession.idToken)
        val newAdmin = try {
            signUp(email, operatorPasswordConfirmation)
        } catch (error: Exception) {
            throw IllegalStateException("No se pudo crear el administrador promovido.", error)
        }

        try {
            firestore.batch()
                .retireOperator(
                    operatorUserId = operatorUserId,
                    newAdminUid = newAdmin.localId,
                    email = email,
                    displayName = displayName,
                )
                .commit()
                .await()
        } catch (error: Exception) {
            deleteAccountBestEffort(newAdmin.idToken)
            throw IllegalStateException(firestoreWriteErrorMessage(error), error)
        }
    }

    private fun WriteBatch.retireOperator(
        operatorUserId: String,
        newAdminUid: String,
        email: String,
        displayName: String,
    ): WriteBatch {
        val operatorRef = firestore.collection(USERS).document(operatorUserId)
        val adminRef = firestore.collection(USERS).document(newAdminUid)
        update(
            operatorRef,
            mapOf(
                "disabledAt" to FieldValue.serverTimestamp(),
                "retiredAt" to FieldValue.serverTimestamp(),
                "promotedToUid" to newAdminUid,
            ),
        )
        set(
            adminRef,
            mapOf(
                "email" to email,
                "displayName" to displayName,
                "role" to ADMIN_ROLE_VALUE,
                "accountCreatedAt" to FieldValue.serverTimestamp(),
                "disabledAt" to null,
                "retiredAt" to null,
                "promotedFromUid" to operatorUserId,
            ),
        )
        return this
    }

    private suspend fun signUp(email: String, password: String): AuthRestSession {
        val response = postIdentityToolkit(
            endpoint = "accounts:signUp",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("returnSecureToken", true),
            credentialErrorMessage = null,
        )
        return response.toSession()
    }

    private suspend fun signIn(
        email: String,
        password: String,
        credentialErrorMessage: String,
    ): AuthRestSession {
        val response = postIdentityToolkit(
            endpoint = "accounts:signInWithPassword",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("returnSecureToken", true),
            credentialErrorMessage = credentialErrorMessage,
        )
        return response.toSession()
    }

    private suspend fun deleteAccount(idToken: String) {
        postIdentityToolkit(
            endpoint = "accounts:delete",
            body = JSONObject().put("idToken", idToken),
            credentialErrorMessage = null,
        )
    }

    private suspend fun deleteAccountBestEffort(idToken: String) {
        runCatching { deleteAccount(idToken) }
    }

    private suspend fun postIdentityToolkit(
        endpoint: String,
        body: JSONObject,
        credentialErrorMessage: String?,
    ): JSONObject = withContext(Dispatchers.IO) {
        val apiKey = Firebase.app.options.apiKey
        if (apiKey.isBlank()) {
            throw IllegalStateException("La app no tiene API key de Firebase configurada.")
        }
        val encodedKey = URLEncoder.encode(apiKey, Charsets.UTF_8.name())
        val connection = URL("$IDENTITY_TOOLKIT_BASE_URL/$endpoint?key=$encodedKey")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = HTTP_TIMEOUT_MILLIS
            connection.readTimeout = HTTP_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val responseJson = if (responseText.isBlank()) JSONObject() else JSONObject(responseText)

            if (responseCode !in 200..299) {
                throw identityToolkitError(responseJson, credentialErrorMessage)
            }

            responseJson
        } catch (error: IOException) {
            throw IllegalStateException("Sin conexion con Firebase Auth. Verifica tu red e intentalo de nuevo.", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toSession(): AuthRestSession {
        val localId = optString("localId")
        val idToken = optString("idToken")
        if (localId.isBlank() || idToken.isBlank()) {
            error("Firebase Auth no devolvio una sesion valida.")
        }
        return AuthRestSession(localId = localId, idToken = idToken)
    }

    private fun identityToolkitError(
        response: JSONObject,
        credentialErrorMessage: String?,
    ): IllegalStateException {
        val code = response.optJSONObject("error")?.optString("message").orEmpty()
        val message = when {
            code == "EMAIL_EXISTS" -> "Ya existe una cuenta con ese correo."
            code == "INVALID_EMAIL" -> "Ingresa un correo valido."
            code.startsWith("WEAK_PASSWORD") -> "La contrasena debe tener al menos 6 caracteres."
            code == "EMAIL_NOT_FOUND" ||
                code == "INVALID_PASSWORD" ||
                code == "INVALID_LOGIN_CREDENTIALS" -> credentialErrorMessage
                ?: "Correo o contrasena incorrectos."
            code == "USER_DISABLED" -> "Esta cuenta esta deshabilitada. Contacta al administrador."
            code == "TOO_MANY_ATTEMPTS_TRY_LATER" ->
                "Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar."
            code == "OPERATION_NOT_ALLOWED" ->
                "El proveedor de correo/contrasena no esta habilitado en Firebase Authentication."
            else -> "No se pudo completar la operacion de usuarios."
        }
        return IllegalStateException(message)
    }

    private fun firestoreWriteErrorMessage(error: Throwable): String =
        if (
            error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            "No tienes permiso para escribir usuarios. Despliega las reglas de Firestore actualizadas."
        } else {
            error.localizedMessage ?: "No se pudo escribir el perfil de usuario."
        }

    private data class AuthRestSession(
        val localId: String,
        val idToken: String,
    )

    private companion object {
        const val USERS = "users"
        const val ADMIN_ROLE_VALUE = "admin"
        const val OPERATOR_ROLE_VALUE = "operator"
        const val IDENTITY_TOOLKIT_BASE_URL = "https://identitytoolkit.googleapis.com/v1"
        const val HTTP_TIMEOUT_MILLIS = 15_000
    }
}
