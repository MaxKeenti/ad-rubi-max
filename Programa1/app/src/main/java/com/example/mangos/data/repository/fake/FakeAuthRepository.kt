package com.example.mangos.data.repository.fake

import com.example.mangos.data.model.User
import com.example.mangos.data.model.UserRole
import com.example.mangos.data.repository.AuthRepository
import com.google.firebase.Timestamp
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val adminUser = User(
        id = "max-uid",
        email = "max@mangos.test",
        displayName = "Max",
        role = UserRole.ADMIN,
        accountCreatedAt = Timestamp.now(),
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<User> {
        _currentUser.value = adminUser
        return Result.success(adminUser)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }

    override suspend fun getUserRole(userId: String): UserRole {
        return if (userId == adminUser.id) adminUser.role else UserRole.OPERATOR
    }
}
