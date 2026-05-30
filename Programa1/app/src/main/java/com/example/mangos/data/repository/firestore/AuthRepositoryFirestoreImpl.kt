package com.example.mangos.data.repository.firestore

import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
            if (fbUser == null) null else loadUserDoc(fbUser.uid)
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    override suspend fun signIn(email: String, password: String): Result<User> {
        return runCatching {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: error("Inicio de sesión sin usuario.")
            loadUserDoc(uid) ?: run {
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
        return User(
            id = uid,
            email = snap.getString("email").orEmpty(),
            displayName = snap.getString("displayName").orEmpty(),
            role = UserRole.fromFirestoreString(snap.getString("role")),
            accountCreatedAt = snap.getTimestamp("accountCreatedAt") ?: Timestamp.now(),
        )
    }

    private companion object {
        const val USERS = "users"
    }
}
