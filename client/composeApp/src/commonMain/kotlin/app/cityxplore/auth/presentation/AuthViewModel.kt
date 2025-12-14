package app.cityxplore.auth.presentation

import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.domain.SocialProvider
import app.cityxplore.platform.CityXploreBaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object Authenticated : AuthState
    data object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(
    private val repository: AuthRepository
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state = _state.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        scope.launch {
            if (repository.isAuthenticated()) {
                _state.value = AuthState.Authenticated
            } else {
                _state.value = AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, pass: String) {
        scope.launch {
            _state.value = AuthState.Loading
            repository.signIn(email, pass)
                .onSuccess { _state.value = AuthState.Authenticated }
                .onFailure { _state.value = AuthState.Error(it.message ?: "Login failed") }
        }
    }

    fun signUp(email: String, pass: String) {
        scope.launch {
            _state.value = AuthState.Loading
            repository.signUp(email, pass)
                .onSuccess {
                    // Usually sign up logs you in, or requires email verification.
                    // For now assume it logs in or we ask to log in.
                    // Supabase default is confirm email.
                    _state.value = AuthState.Error("Check your email to confirm account")
                }
                .onFailure { _state.value = AuthState.Error(it.message ?: "Registration failed") }
        }
    }

    fun onSocialLogin(provider: SocialProvider) {
        scope.launch {
            _state.value = AuthState.Loading
            repository.signInWith(provider)
                .onSuccess { _state.value = AuthState.Authenticated }
                .onFailure { _state.value = AuthState.Error(it.message ?: "Social login failed") }
        }
    }

    fun signOut() {
        scope.launch {
            repository.signOut()
            _state.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }
}
