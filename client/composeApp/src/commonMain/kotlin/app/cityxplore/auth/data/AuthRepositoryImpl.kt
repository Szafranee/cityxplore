package app.cityxplore.auth.data

import app.cityxplore.BuildConfig
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
import io.github.jan.supabase.auth.user.UserSession
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Implementation of the AuthRepository interface, providing methods for
 * user authentication and session management using Supabase.
 *
 * This class handles various authentication-related actions such as signing in,
 * signing out, user session retrieval, and profile verification.
 *
 * It integrates with the SupabaseClient for authentication and the HttpClient
 * for performing additional network requests, such as verifying user profiles
 * or handling username-based logins via edge functions.
 *
 * It also ensures local data integrity during authentication flows by using
 * the LocalDataCleaner to clear user-specific cached data during sign-out.
 */
class AuthRepositoryImpl(
    supabase: SupabaseClient,
    private val client: HttpClient,
    private val localDataCleaner: LocalDataCleaner
) : AuthRepository {
    /**
     * Provides access to the authentication functionality of the Supabase client.
     *
     * This property is a reference to the authentication module of the Supabase SDK.
     * It ensures interaction with user authentication mechanisms such as sign-in,
     * sign-up, session management, and authentication state validation.
     *
     * Common operations that rely on this property include:
     * - User login using email-password or social providers
     * - User account sign-up with email verification support
     * - Signing out and clearing associated local data
     * - Checking the current authentication state of a user
     * - Fetching the current user's ID or session details
     * - Triggering specific authentication-related actions, such as resending
     *   verification emails or importing existing sessions
     *
     * This property is critical for maintaining secure and seamless communication
     * with the Supabase backend for authentication-related workflows.
     * It directly interacts with the `supabase.auth` API to perform these operations.
     */
    private val auth = supabase.auth

    /**
     * A Flow representing the current authentication state of the user.
     *
     * Emits the following values:
     * - `true` if the user is authenticated.
     * - `false` if the user is not authenticated.
     * - `null` if the session is still in the initialising state.
     *
     * The state is derived from the `sessionStatus` of the authentication system.
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
     * Signs in a user using a specified social authentication provider.
     *
     * @param provider The [SocialProvider] to use for authentication. Can be either [SocialProvider.GOOGLE] or [SocialProvider.DISCORD].
     * @return [Result] containing [Unit] on success, or an exception on failure.
     */
    override suspend fun signInWith(provider: SocialProvider): Result<Unit> {
        return try {
            val redirectUrl = "app.cityxplore://login"
            when (provider) {
                SocialProvider.GOOGLE -> auth.signInWith(Google, redirectUrl)
                SocialProvider.DISCORD -> auth.signInWith(Discord, redirectUrl)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registers a new user with the provided email and password.
     *
     * If the user is successfully registered, a successful result is returned.
     * If the email address is already associated with an account, an [EmailAlreadyRegisteredException]
     * is returned in the result.
     *
     * @param email The email address of the user to register.
     * @param password The password for the new user account.
     * @return [Result] containing [Unit] on successful registration, or an exception on failure.
     */
    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            val result = auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password.trim()
            }
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
     * Signs in a user using the provided email and password.
     *
     * Attempts to authenticate the user with the given email and password credentials.
     * On successful authentication, returns a successful [Result].
     * If the authentication fails, returns a failed [Result] containing the exception encountered.
     *
     * @param email The email address of the user attempting to sign in.
     * @param password The password associated with the given email.
     * @return A [Result] object containing a [Unit] on success or an [Exception] on failure.
     */
    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password.trim()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the currently authenticated user by clearing all local user data,
     * invalidating the session, and handling any clean-up errors.
     *
     * @return [Result] containing [Unit] on success, or an exception on failure.
     */
    override suspend fun signOut(): Result<Unit> = runCatching {
        var cleanupError: Throwable? = null
        try {
            localDataCleaner.clearAllUserData()
        } catch (t: Throwable) {
            cleanupError = t
        }

        auth.signOut()
        cleanupError?.let { throw it }
    }

    /**
     * Checks if a user is currently authenticated by verifying the existence of a valid session.
     *
     * @return `true` if a valid authentication session exists; otherwise `false`.
     */
    override suspend fun isAuthenticated(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    /**
     * Attempts to sign in a user using either an email or a username as the login.
     *
     * If the login includes an "@" symbol, it is treated as an email and the default sign-in process is used.
     * Otherwise, a custom edge function is invoked to authenticate with a username.
     *
     * @param login The login credential, which can either be an email or a username.
     * @param password The password associated with the given login credential.
     * @return A [Result] indicating success (with [Unit]) if the sign-in is successful, or failure with an [Exception].
     */
    override suspend fun signInWithLogin(login: String, password: String): Result<Unit> {
        val trimmedLogin = login.trim()
        val trimmedPassword = password.trim()

        return try {
            if (trimmedLogin.contains("@")) {
                return signIn(trimmedLogin, trimmedPassword)
            }

            // Using Edge Function for Username login via Raw HTTP
            val requestBody = LoginWithUsernameRequest(trimmedLogin, trimmedPassword)
            val functionUrl = "${BuildConfig.SUPABASE_URL}/functions/v1/login-with-username"
            val apiKey = BuildConfig.SUPABASE_KEY

            val response = client.post(functionUrl) {
                expectSuccess = false
                header("apikey", apiKey)
                bearerAuth(apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status != HttpStatusCode.OK) {
                val errorBody = runCatching { response.body<ErrorResponse>() }.getOrNull()
                val errorMsg = errorBody?.error ?: "Authentication failed with status ${response.status}"

                return Result.failure(Exception(errorMsg))
            }

            val sessionDto = response.body<EdgeFunctionSessionDto>()
            val session = sessionDto.session

            if (session != null) {
                auth.importSession(session)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Authentication failed: No session returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks whether the currently authenticated user has a profile in the backend service.
     *
     * This method sends a network request to verify the existence of the user's profile.
     * It handles various HTTP responses, including 404 (profile not found) and 401/403
     * (invalid or expired session).
     *
     * @return [Result] containing `true` if the user has a profile, `false` if not,
     *         or failure if the operation cannot be completed (e.g., network issues or session errors).
     */
    override suspend fun hasProfile(): Result<Boolean> {
        auth.currentUserOrNull() ?: return Result.success(false)
        return try {
            val response = client.get("https://api.cityxplore.app/api/users/me")
            Result.success(response.status == HttpStatusCode.OK)
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.NotFound -> Result.success(false)
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                    Result.failure(InvalidSessionException("Session expired or user no longer exists"))
                }

                else -> Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resends the email verification link to the specified email address.
     *
     * @param email The email address to which the verification link will be sent.
     * @return [Result] containing [Unit] on success, or an exception on failure.
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
     * The method checks if there is a user currently authenticated and, if so,
     * returns the user's unique ID. If no user is authenticated, it returns `null`.
     *
     * @return The user ID as a [String], or `null` if no user is authenticated.
     */
    override suspend fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
}

/**
 * Exception thrown when a user's session is deemed invalid.
 *
 * This typically occurs when:
 * - The user's session has expired.
 * - The user no longer exists in the backend system.
 *
 * It is commonly used in authentication flows to signal session-related issues
 * and prompt appropriate recovery actions, such as forcing a re-login.
 *
 * @param message The detail message describing the reason for the invalid session.
 */
class InvalidSessionException(message: String) : Exception(message)

/**
 * Exception thrown to indicate that an account with the specified email address
 * is already registered.
 *
 * This exception is typically used in authentication or registration workflows
 * to signal that the email address provided during the sign-up process is already
 * associated with an existing user account.
 */
class EmailAlreadyRegisteredException : Exception("An account with this email already exists.")

/**
 * Data class representing the request payload for signing in using a username or email and password.
 *
 * Used in the `signInWithLogin` method of `AuthRepositoryImpl` to send a request
 * to the Edge Function responsible for resolving the username or email and authenticating the user.
 *
 * @property login The username or email of the user.
 * @property password The password of the user.
 */
// Request/Response DTOs for Edge Function
@Serializable
private data class LoginWithUsernameRequest(
    val login: String,
    val password: String
)

/**
 * Represents an error response structure typically returned by the server.
 *
 * This class is used to deserialize error responses, providing a structured
 * way to handle error messages within the application. The `error` field contains
 * the error message as a string.
 */
@Serializable
private data class ErrorResponse(
    val error: String
)

/**
 * Data Transfer Object (DTO) representing the response of an Edge Function
 * that returns a user session.
 *
 * This DTO is used to deserialize the response payload of the Edge Function
 * associated with authentication mechanisms, such as logging in with a username.
 *
 * @property session Represents the user session data returned by the Edge Function,
 * or `null` if no session is available or the authentication process failed.
 */
@Serializable
private data class EdgeFunctionSessionDto(
    @SerialName("session") val session: UserSession? = null
)
