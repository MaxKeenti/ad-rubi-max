package com.example.mangos.data.repository

import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>

    suspend fun signIn(email: String, password: String): Result<User>

    suspend fun signOut()

    suspend fun getUserRole(userId: String): UserRole
}
