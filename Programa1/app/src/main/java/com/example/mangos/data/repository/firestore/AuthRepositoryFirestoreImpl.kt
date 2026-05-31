package com.example.mangos.data.repository.firestore

import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepositoryFirestoreImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: StateFlow<User?> = authStateFlow
        .mapLatest { fbUser ->
            if (fbUser == null) {
                null
            } else {
                runCatching { loadUserDoc(fbUser.uid) }
                    .getOrElse {
                        auth.signOut()
                        null
                    }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    override suspend fun signIn(email: String, password: String): Result<User> {
        return runCatching {
            val authResult = try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (error: FirebaseAuthException) {
                throw IllegalStateException(authErrorMessage(error), error)
            }
            val uid = authResult.user?.uid
                ?: error("Inicio de sesión sin usuario.")
            val user = try {
                loadUserDoc(uid)
            } catch (error: Exception) {
                auth.signOut()
                throw IllegalStateException(profileLoadErrorMessage(uid, error), error)
            }
            user ?: run {
                auth.signOut()
                error("Esta cuenta no tiene perfil en Firestore. Contacta al administrador.")
            }
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun getUserRole(userId: String): UserRole {
        val snap = firestore.collection(USERS).document(userId).get().await()
        return UserRole.fromFirestoreString(snap.getString("role"))
    }

    private suspend fun loadUserDoc(uid: String): User? {
        val snap = firestore.collection(USERS).document(uid).get().await()
        if (!snap.exists()) return null
        val disabledAt = snap.getTimestamp("disabledAt")
        val retiredAt = snap.getTimestamp("retiredAt")
        if (disabledAt != null || retiredAt != null) {
            throw IllegalStateException("Esta cuenta esta deshabilitada. Contacta al administrador.")
        }
        return User(
            id = uid,
            email = snap.getString("email").orEmpty(),
            displayName = snap.getString("displayName").orEmpty(),
            role = UserRole.fromFirestoreString(snap.getString("role")),
            accountCreatedAt = snap.getTimestamp("accountCreatedAt") ?: Timestamp.now(),
            disabledAt = disabledAt,
            retiredAt = retiredAt,
            promotedToUid = snap.getString("promotedToUid"),
            promotedFromUid = snap.getString("promotedFromUid"),
        )
    }

    private fun authErrorMessage(error: FirebaseAuthException): String = when (error.errorCode) {
        "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido."
        "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada. Contacta al administrador."
        "ERROR_USER_NOT_FOUND",
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_LOGIN_CREDENTIALS" ->
            "Correo o contraseña incorrectos."
        "ERROR_TOO_MANY_REQUESTS" ->
            "Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión. Verifica tu red e inténtalo de nuevo."
        else -> error.localizedMessage ?: "No se pudo iniciar sesión."
    }

    private fun profileLoadErrorMessage(uid: String, error: Throwable): String {
        return if (
            error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            "No se pudo leer tu perfil. Verifica que exista users/$uid para esta cuenta y que las reglas de Firestore permitan leerlo."
        } else {
            error.localizedMessage ?: "No se pudo leer tu perfil."
        }
    }

    private companion object {
        const val USERS = "users"
    }
}
