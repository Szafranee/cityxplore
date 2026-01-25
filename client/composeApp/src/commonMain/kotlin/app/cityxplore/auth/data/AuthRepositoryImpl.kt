package app.cityxplore.auth.data

import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.domain.SocialProvider
import app.cityxplore.core.data.LocalDataCleaner
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Discord
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlin.time.TimeSource

/**
 * Implementation of [AuthRepository] using Supabase Auth SDK and Ktor HTTP client.
 *
 * This class manages authentication operations including email/password authentication,
 * social provider authentication (Google, Discord), session management, and user profile verification.
 *
 * @property supabase The Supabase client instance for authentication operations.
 * @property client The HTTP client for making API calls to the backend.
 * @property localDataCleaner Service to clear local cached data on sign-out.
 */
class AuthRepositoryImpl(
    private val supabase: SupabaseClient,
    private val client: HttpClient,
    private val localDataCleaner: LocalDataCleaner
) : AuthRepository {
    private val auth = supabase.auth

    /**
     * Flow emitting authentication state based on Supabase session status.
     * Emits `true` when a session is authenticated, `false` when not authenticated,
     * and `null` when the session is still initialising (to avoid premature navigation).
     */
    override val authState: Flow<Boolean?> = auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> true
                is SessionStatus.Initializing -> null
                else -> false
            }
        }

    /**
     * Initiates social provider authentication using the specified provider.
     * Opens the provider's OAuth flow with a deep link redirect URL.
     *
     * @param provider The social authentication provider to use.
     * @return [Result] containing [Unit] on successful initiation, or exception on failure.
     */
    override suspend fun signInWith(provider: SocialProvider): Result<Unit> {
        return try {
            val redirectUrl = "app.cityxplore://login"
            when (provider) {
                SocialProvider.GOOGLE -> auth.signInWith(Google, redirectUrl)
                // SocialProvider.FACEBOOK -> auth.signInWith(Facebook, redirectUrl)
                SocialProvider.DISCORD -> auth.signInWith(Discord, redirectUrl)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registers a new user with email and password using Supabase Auth.
     *
     * The user will receive a verification email before they can sign in.
     *
     * **Note**: Supabase does not return an error for duplicate emails for security reasons.
     * Instead, it returns a user object with an empty `identities` list. This method
     * detects this case and returns an appropriate error.
     *
     * @param email The user's email address.
     * @param password The user's password (minimum 6 characters).
     * @return [Result] containing [Unit] on success, or [EmailAlreadyRegisteredException] if
     *         the email is already registered.
     */
    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            val result = auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // Supabase returns user with an empty identities list if the email already exists
            // instead of returning an error (for security/privacy reasons)
            if (result?.identities.isNullOrEmpty()) {
                Result.failure(EmailAlreadyRegisteredException())
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user with email and password.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the currently authenticated user, clearing the session and all local data.
     *
     * This method first clears all cached local data to prevent data leakage
     * when switching between accounts, then signs out from Supabase.
     *
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    override suspend fun signOut(): Result<Unit> = runCatching {
        var cleanupError: Throwable? = null
        try {
            // Clear all local data BEFORE signing out to prevent data leakage
            localDataCleaner.clearAllUserData()
        } catch (t: Throwable) {
            cleanupError = t
        }

        // Always attempt remote sign-out
        auth.signOut()

        cleanupError?.let { throw it }
    }

    /**
     * Checks if a valid authentication session exists.
     *
     * @return `true` if authenticated, `false` otherwise.
     */
    override suspend fun isAuthenticated(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    /**
     * Resolves a login identifier to an email address.
     * If the input contains '@', it is assumed to be an email and returned as-is.
     * Otherwise, the username is queried in the Supabase users table to find the associated email.
     *
     * **Security Note**: This method includes a deliberate delay to mitigate timing attacks
     * and username enumeration. Always returns null for non-existent usernames to avoid
     * revealing whether a username exists in the system.
     *
     * @param login The username or email to resolve.
     * @return The resolved email address, or `null` if the username is not found.
     */
    override suspend fun resolveEmail(login: String): String? {
        if (login.contains("@")) return login

        // Add deliberate delay to prevent timing-based username enumeration
        val startMark = TimeSource.Monotonic.markNow()

        // Query users table to find email by username
        val result = try {
            val user = supabase.postgrest.from("users")
                .select {
                    filter {
                        eq("username", login)
                    }
                }.decodeSingleOrNull<UserEmailDto>()
            user?.email
        } catch (_: Exception) {
            null
        }

        // Ensure a consistent response time (minimum 200 ms) to prevent timing attacks
        val elapsed = startMark.elapsedNow().inWholeMilliseconds
        if (elapsed < 200) {
            delay(200 - elapsed)
        }

        return result
    }

    /**
     * Checks if the currently authenticated user has a profile in the backend system.
     * Makes a request to the `/api/users/me` endpoint to verify profile existence.
     *
     * @return [Result] containing `true` if the user has a profile (200 OK response),
     *         `false` if not (404), or failure if the request couldn't be made (e.g., no network).
     *         Also returns failure for 401/403 (invalid session) to trigger re-authentication.
     */
    override suspend fun hasProfile(): Result<Boolean> {
        auth.currentUserOrNull() ?: return Result.success(false)
        return try {
            val response = client.get("https://api.cityxplore.app/api/users/me")
            Result.success(response.status == HttpStatusCode.OK)
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                // 404 = user doesn't have a profile yet (needs onboarding)
                HttpStatusCode.NotFound -> Result.success(false)
                // 401/403 = session is invalid or user doesn't exist - force re-auth
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                    println("AuthRepository: Session invalid (${e.response.status}), clearing session")
                    Result.failure(InvalidSessionException("Session expired or user no longer exists"))
                }

                else -> Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exception thrown when the session is invalid (expired or user deleted).
     */
    class InvalidSessionException(message: String) : Exception(message)

    /**
     * Resends the email verification link to the specified email address.
     *
     * @param email The email address to send the verification link to.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return try {
            auth.resendEmail(OtpType.Email.SIGNUP, email)
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(Exception("Failed to resend verification email"))
        }
    }

    /**
     * Retrieves the unique identifier of the currently authenticated user.
     *
     * @return The user ID as a [String], or `null` if not authenticated.
     */
    override suspend fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
}

/**
 * Data transfer object for retrieving user email from the database.
 *
 * @property email The user's email address.
 */
@Serializable
data class UserEmailDto(val email: String)

/**
 * Exception thrown when attempting to register with an email that is already in use.
 *
 * This exception is used to provide clear feedback to users when they try to sign up
 * with an email address that already exists in the authentication system.
 */
class EmailAlreadyRegisteredException : Exception("An account with this email already exists.")
