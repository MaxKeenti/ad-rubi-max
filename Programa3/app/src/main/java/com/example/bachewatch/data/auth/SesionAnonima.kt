package com.example.bachewatch.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Anonymous Firebase session (ADR-0001): silent sign-in on first
 * launch; the uid is what rules constrain and `createdBy` records.
 */
interface SesionAnonima {
    val uid: StateFlow<String?>
    suspend fun ensureSignedIn(): Result<String>
}
