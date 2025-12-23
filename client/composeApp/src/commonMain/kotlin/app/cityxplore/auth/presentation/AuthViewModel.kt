package app.cityxplore.auth.presentation

import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.domain.SocialProvider
import app.cityxplore.platform.CityXploreBaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object Authenticated : AuthState
    data object Unauthenticated : AuthState
    data object Onboarding : AuthState
    data class EmailVerification(val email: String) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(
    private val repository: AuthRepository
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state = _state.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId = _userId.asStateFlow()

    init {
        observeAuthState()
        observeUserId()
    }

    private fun observeUserId() {
        scope.launch {
            repository.authState.collect { isAuthenticated ->
                _userId.value = if (isAuthenticated) {
                    repository.getCurrentUserId()
                } else {
                    null
                }
            }
        }
    }

    private fun observeAuthState() {
        scope.launch {
            repository.authState.collect { isAuthenticated ->
                if (isAuthenticated) {
                    // Allow session and JWT token to be fully initialised before making API calls
                    delay(500)
                    if (repository.hasProfile()) {
                        _state.value = AuthState.Authenticated
                    } else {
                        _state.value = AuthState.Onboarding
                    }
                } else {
                    _state.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun signIn(login: String, pass: String) {
        scope.launch {
            _state.value = AuthState.Loading
            val email = repository.resolveEmail(login)
            if (email == null) {
                _state.value = AuthState.Error("Could not find account with that username.")
                return@launch
            }
            repository.signIn(email, pass)
                .onSuccess {
                    // State update handled by observeAuthState
                }
                .onFailure { error ->
                    val message = parseAuthError(error)
                    _state.value = AuthState.Error(message)
                }
        }
    }

    fun signUp(email: String, pass: String) {
        scope.launch {
            _state.value = AuthState.Loading
            repository.signUp(email, pass)
                .onSuccess {
                    if (repository.isAuthenticated()) {
                        // observeAuthState will handle navigation
                    } else {
                        _state.value = AuthState.EmailVerification(email)
                        startVerificationPolling()
                    }
                }
                .onFailure { error ->
                    val message = parseAuthError(error)
                    _state.value = AuthState.Error(message)
                }
        }
    }

    // Poll for email verification status to support verification on different devices
    private fun startVerificationPolling() {
        scope.launch {
            while (_state.value is AuthState.EmailVerification) {
                delay(3000)
                if (repository.isAuthenticated()) {
                    break
                }
            }
        }
    }

    fun resendVerificationEmail(email: String) {
        scope.launch {
            repository.resendVerificationEmail(email)
        }
    }

    fun cancelVerification() {
        _state.value = AuthState.Unauthenticated
    }

    fun onSocialLogin(provider: SocialProvider) {
        scope.launch {
            _state.value = AuthState.Loading
            repository.signInWith(provider)
                .onFailure { error ->
                    val message = parseAuthError(error)
                    _state.value = AuthState.Error(message)
                }
        }
    }

    private fun parseAuthError(error: Throwable): String {
        val msg = error.message ?: return "An unknown error occurred"
        return when {
            msg.contains("invalid_grant", ignoreCase = true) || msg.contains(
                "Invalid login credentials",
                ignoreCase = true
            ) -> "Invalid email or password."

            msg.contains("user_already_exists", ignoreCase = true) -> "User already exists."
            msg.contains("placeholder.supabase.co", ignoreCase = true) -> "Invalid Supabase URL configuration."
            msg.contains("hostname", ignoreCase = true) || msg.contains(
                "ConnectException",
                ignoreCase = true
            ) -> "Network error. Check your connection."

            msg.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            else -> "Authentication failed: $msg"
        }
    }

    fun signOut() {
        scope.launch {
            repository.signOut()
            _state.value = AuthState.Unauthenticated
        }
    }

    fun refreshProfileCheck() {
        scope.launch {
            if (repository.isAuthenticated()) {
                if (repository.hasProfile()) {
                    _state.value = AuthState.Authenticated
                } else {
                    _state.value = AuthState.Onboarding
                }
            }
        }
    }

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }
}
