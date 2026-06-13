package com.example.bachewatch.data.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class FakeSesionAnonima @Inject constructor() : SesionAnonima {

    companion object {
        /** Seed data marks two reportes with this uid so Eliminar is demoable. */
        const val FAKE_UID = "fake-uid-local"
    }

    private val _uid = MutableStateFlow<String?>(FAKE_UID)
    override val uid: StateFlow<String?> = _uid

    override suspend fun ensureSignedIn(): Result<String> = Result.success(FAKE_UID)
}
