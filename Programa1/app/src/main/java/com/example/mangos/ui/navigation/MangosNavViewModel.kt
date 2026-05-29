package com.example.mangos.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.mangos.data.model.User
import com.example.mangos.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class MangosNavViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    val currentUser: StateFlow<User?> = authRepository.currentUser
}
