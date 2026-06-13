package com.example.bachewatch.data.auth

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Anonymous Firebase session (ADR-0001). Sign-in is silent and one-shot;
 * the uid mirrors an [FirebaseAuth.AuthStateListener] so callers observe
 * sign-in completing. Rules require a signed-in user even for reads, so
 * [ensureSignedIn] runs at app start (see BacheWatchApp).
 */
@Singleton
class SesionAnonimaFirebase @Inject constructor(
    private val auth: FirebaseAuth,
) : SesionAnonima {

    private val _uid = MutableStateFlow(auth.currentUser?.uid)
    override val uid: StateFlow<String?> = _uid.asStateFlow()

    init {
        auth.addAuthStateListener { fa -> _uid.value = fa.currentUser?.uid }
    }

    override suspend fun ensureSignedIn(): Result<String> = runCatching {
        auth.currentUser?.uid
            ?: auth.signInAnonymously().await().user?.uid
            ?: error("Sign-in anónimo no devolvió un usuario")
    }
}
