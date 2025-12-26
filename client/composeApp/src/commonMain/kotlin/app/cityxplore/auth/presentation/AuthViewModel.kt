package app.cityxplore.auth.presentation

import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.domain.SocialProvider
import app.cityxplore.platform.CityXploreBaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed interface representing the authentication state of the application.
 *
 * This state machine controls navigation between authentication screens
 * and determines whether the user can access the main application features.
 */
sealed interface AuthState {
    /** Initial loading state while verifying authentication status */
    data object Loading : AuthState

    /** User is authenticated and has a complete profile */
    data object Authenticated : AuthState

    /** User is not authenticated */
    data object Unauthenticated : AuthState

    /** User is authenticated but needs to complete onboarding/profile setup */
    data object Onboarding : AuthState

    /** User needs to verify their email before proceeding */
    data class EmailVerification(val email: String) : AuthState

    /** An error occurred during authentication */
    data class Error(val message: String) : AuthState
}

/**
 * ViewModel managing authentication state and operations.
 *
 * This ViewModel handles user sign-in, sign-up, social authentication,
 * email verification, and session management. It observes the authentication
 * state from the repository and updates the UI accordingly.
 *
 * @property repository The authentication repository for backend operations.
 */
class AuthViewModel(
    private val repository: AuthRepository
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)

    /**
     * StateFlow emitting the current authentication state.
     * UI components observe this to react to authentication changes.
     */
    val state = _state.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)

    /**
     * StateFlow emitting the current user's unique identifier.
     * Emits `null` when not authenticated.
     */
    val userId = _userId.asStateFlow()

    init {
        observeAuthState()
        observeUserId()
    }

    /**
     * Observes the user ID from the authentication state.
     * Updates whenever authentication status changes.
     */
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

    /**
     * Observes the authentication state from the repository.
     * When authenticated, checks if the user has a profile to determine
     * if onboarding is required.
     */
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

    /**
     * Signs in a user with username or email and password.
     * If a username is provided, it is resolved to an email address first.
     *
     * @param login The username or email address.
     * @param pass The user's password.
     */
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

    /**
     * Registers a new user with email and password.
     * If the user is immediately authenticated (no email verification required),
     * proceeds to check profile status. Otherwise, enters email verification state.
     *
     * @param email The user's email address.
     * @param pass The user's password (minimum 6 characters).
     */
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

    /**
     * Polls for email verification status to support verification on different devices.
     * Checks authentication status every 3 seconds until verified.
     */
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

    /**
     * Resends the email verification link to the specified email address.
     *
     * @param email The email address to send the verification link to.
     */
    fun resendVerificationEmail(email: String) {
        scope.launch {
            repository.resendVerificationEmail(email)
        }
    }

    /**
     * Cancels email verification and returns to the unauthenticated state.
     */
    fun cancelVerification() {
        _state.value = AuthState.Unauthenticated
    }

    /**
     * Initiates social provider authentication (Google, Discord).
     *
     * @param provider The [SocialProvider] to use for authentication.
     */
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

    /**
     * Parses authentication errors into user-friendly messages.
     *
     * @param error The exception thrown during authentication.
     * @return A user-friendly error message.
     */
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

    /**
     * Signs out the currently authenticated user.
     */
    fun signOut() {
        scope.launch {
            repository.signOut()
            _state.value = AuthState.Unauthenticated
        }
    }

    /**
     * Refreshes the profile check to determine if onboarding is required.
     * Useful after profile creation to update the authentication state.
     */
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

    /**
     * Clears the current error state and returns to unauthenticated state.
     */
    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }
}
