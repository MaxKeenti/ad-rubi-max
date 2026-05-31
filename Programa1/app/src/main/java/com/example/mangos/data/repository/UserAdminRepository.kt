package com.example.mangos.data.repository

import com.example.mangos.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserAdminRepository {
    fun observeOperators(): Flow<List<User>>

    suspend fun refreshOperators(): Result<Unit>

    suspend fun createOperator(
        email: String,
        displayName: String,
        password: String,
    ): Result<Unit>

    suspend fun createAdmin(
        email: String,
        displayName: String,
        password: String,
        adminPasswordConfirmation: String,
    ): Result<Unit>

    suspend fun promoteOperatorToAdmin(
        operatorUserId: String,
        operatorPasswordConfirmation: String,
        adminPasswordConfirmation: String,
    ): Result<Unit>
}
