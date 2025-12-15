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
        observeAuthState()
    }

    private fun observeAuthState() {
        scope.launch {
            repository.authState.collect { isAuthenticated ->
                if (isAuthenticated) {
                    println("AuthViewModel: User is authenticated, navigating to Map.")
                    _state.value = AuthState.Authenticated
                } else {
                    _state.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun signIn(email: String, pass: String) {
        scope.launch {
            _state.value = AuthState.Loading
            repository.signIn(email, pass)
                .onSuccess { _state.value = AuthState.Authenticated }
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
                    // Usually sign up logs you in, or requires email verification.
                    // For now assume it logs in or we ask to log in.
                    // Supabase default is confirm email.
                    _state.value = AuthState.Error("Check your email to confirm account")
                }
                .onFailure { error ->
                    val message = parseAuthError(error)
                    _state.value = AuthState.Error(message)
                }
        }
    }

    fun onSocialLogin(provider: SocialProvider) {
        scope.launch {
            _state.value = AuthState.Loading
            repository.signInWith(provider)
                .onSuccess {
                    // Do not set Authenticated here. OAuth flow continues in browser.
                    // The app will receive a deep link, which Supabase handles.
                    // We should listen to session changes to detect when login completes.
                    println("Social login initiated for $provider")
                }
                .onFailure { error ->
                    val message = parseAuthError(error)
                    _state.value = AuthState.Error(message)
                }
        }
    }

    private fun parseAuthError(error: Throwable): String {
        val msg = error.message ?: return "An unknown error occurred"
        println("Auth Error: $msg") // Log error for debugging
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

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }
}
