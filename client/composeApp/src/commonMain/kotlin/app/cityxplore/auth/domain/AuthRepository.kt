package app.cityxplore.auth.domain

import kotlinx.coroutines.flow.Flow

/**
 * Enumeration representing available social authentication providers.
 */
enum class SocialProvider {
    /** Google OAuth provider */
    GOOGLE,

    /** Discord OAuth provider */
    DISCORD
}

/**
 * Repository interface for authentication operations using Supabase Auth.
 *
 * This repository handles user authentication, including email/password sign-in,
 * social provider authentication (Google, Discord), session management, and profile verification.
 *
 * @see app.cityxplore.auth.data.AuthRepositoryImpl
 */
interface AuthRepository {
    /**
     * Flow emitting the current authentication state of the user.
     * Emits `true` when authenticated, `false` when not authenticated,
     * and `null` when the session is still initialising.
     */
    val authState: Flow<Boolean?>

    /**
     * Registers a new user with email and password.
     *
     * @param email The user's email address.
     * @param password The user's password (minimum [AuthConstants.MIN_PASSWORD_LENGTH] characters).
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun signUp(email: String, password: String): Result<Unit>

    /**
     * Signs in an existing user with email and password.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun signIn(email: String, password: String): Result<Unit>

    /**
     * Initiates social provider authentication flow.
     *
     * @param provider The [SocialProvider] to use for authentication.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun signInWith(provider: SocialProvider): Result<Unit>

    /**
     * Signs out the currently authenticated user.
     *
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Checks if a user is currently authenticated with a valid session.
     *
     * @return `true` if authenticated, `false` otherwise.
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Signs in using a login identifier (username or email) and password.
     *
     * This method uses an Edge Function to safely resolve the username to an email
     * server-side and perform the sign-in in a single request, avoiding
     * exposure of user emails to the client.
     *
     * @param login The username or email.
     * @param password The user's password.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun signInWithLogin(login: String, password: String): Result<Unit>


    /**
     * Checks if the currently authenticated user has a profile in the backend.
     *
     * @return [Result] containing `true` if the user has a profile, `false` if not,
     *         or failure if the check couldn't be performed (e.g., no network).
     */
    suspend fun hasProfile(): Result<Boolean>

    /**
     * Resends the email verification link to the specified email address.
     *
     * @param email The email address to send the verification link to.
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    suspend fun resendVerificationEmail(email: String): Result<Unit>

    /**
     * Retrieves the unique identifier of the currently authenticated user.
     *
     * @return The user ID as a [String], or `null` if not authenticated.
     */
    suspend fun getCurrentUserId(): String?
}
