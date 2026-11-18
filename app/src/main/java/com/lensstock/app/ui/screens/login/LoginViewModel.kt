package com.lensstock.app.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lensstock.app.R
import com.lensstock.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
    data object SignedIn : LoginUiState
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun signIn() {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val webClientId = getApplication<Application>().getString(R.string.default_web_client_id)
                authRepository.signIn(webClientId)
                _uiState.value = LoginUiState.SignedIn
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Sign-in failed")
            }
        }
    }
}
