package app.cityxplore.auth.presentation

import app.cityxplore.auth.data.EmailAlreadyRegisteredException
import app.cityxplore.auth.domain.AuthConstants
import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.domain.SocialProvider
import app.cityxplore.core.cache.CacheManager
import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.social.domain.repository.SharedPoiRepository
import app.cityxplore.social.domain.repository.SocialRepository
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
    private val repository: AuthRepository,
    private val sharedPoiRepository: SharedPoiRepository,
    private val socialRepository: SocialRepository,
    private val cacheManager: CacheManager
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
     * Updates whenever the authentication status changes.
     */
    private fun observeUserId() {
        scope.launch {
            repository.authState.collect { isAuthenticated ->
                _userId.value = if (isAuthenticated == true) {
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
     * if onboarding is required. Stays in Loading state during session initialisation.
     *
     * If the profile check fails due to network issues, the user is signed out
     * and returned to the login screen to avoid being stuck on onboarding without the internet.
     */
    private fun observeAuthState() {
        scope.launch {
            repository.authState.collect { isAuthenticated ->
                when (isAuthenticated) {
                    true -> {
                        // Check if a user has a profile
                        repository.hasProfile()
                            .onSuccess { hasProfile ->
                                if (hasProfile) {
                                    _state.value = AuthState.Authenticated
                                } else {
                                    _state.value = AuthState.Onboarding
                                }
                            }
                            .onFailure { error ->
                                // Session invalid or network error - sign out and return to log in
                                println("AuthViewModel: Profile check failed: ${error.message}")
                                repository.signOut()
                                _state.value = AuthState.Error(
                                    "Session expired or account no longer exists. Please sign in again."
                                )
                            }
                    }

                    false -> {
                        _state.value = AuthState.Unauthenticated
                    }

                    null -> {
                        // Session is still initialising, keep Loading state
                        _state.value = AuthState.Loading
                    }
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
            repository.signInWithLogin(login, pass)
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
     *
     * If the user is immediately authenticated (no email verification required),
     * proceeds to check profile status. Otherwise, enters email verification state.
     *
     * **Note**: Supabase does not return an error for duplicate emails for security reasons.
     * The repository layer handles this by checking the returned `identities` list.
     *
     * @param email The user's email address.
     * @param pass The user's password (minimum [AuthConstants.MIN_PASSWORD_LENGTH] characters).
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
     * Handles common authentication errors including network issues, invalid credentials,
     * duplicate accounts, and configuration problems.
     *
     * @param error The exception thrown during authentication.
     * @return A user-friendly error message suitable for display to the user.
     */
    private fun parseAuthError(error: Throwable): String {
        // Handle specific exception types first
        if (error is EmailAlreadyRegisteredException) {
            return error.message ?: "An account with this email already exists."
        }

        val msg = error.message ?: return "An unknown error occurred"
        return when {
            msg.contains("invalid_grant", ignoreCase = true) || msg.contains(
                "Invalid login credentials",
                ignoreCase = true
            ) -> "Invalid email or password."

            msg.contains("user_already_exists", ignoreCase = true) ||
                    msg.contains("already exists", ignoreCase = true) -> "An account with this email already exists."

            msg.contains("placeholder.supabase.co", ignoreCase = true) -> "Invalid Supabase URL configuration."
            msg.contains("hostname", ignoreCase = true) || msg.contains(
                "ConnectException",
                ignoreCase = true
            ) -> "Network error. Check your connection."

            msg.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            msg.contains("email_not_confirmed", ignoreCase = true) -> "Email not confirmed. Please check your inbox."
            else -> "Authentication failed: $msg"
        }
    }

    /**
     * Signs out the currently authenticated user.
     */
    fun signOut() {
        scope.launch {
            repository.signOut()
            // Clear all cached data from social repositories
            sharedPoiRepository.clearCache()
            socialRepository.clearCache()
            cacheManager.clearAll()
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
                repository.hasProfile()
                    .onSuccess { hasProfile ->
                        if (hasProfile) {
                            _state.value = AuthState.Authenticated
                        } else {
                            _state.value = AuthState.Onboarding
                        }
                    }
                    .onFailure {
                        // Network error - show error state
                        _state.value =
                            AuthState.Error("Unable to verify profile. Please check your internet connection.")
                    }
            }
        }
    }

    /**
     * Clears the current error state and returns to the unauthenticated state.
     */
    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }
}
