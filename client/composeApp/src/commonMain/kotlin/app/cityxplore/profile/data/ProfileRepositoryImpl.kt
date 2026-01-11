package app.cityxplore.profile.data

import app.cityxplore.profile.domain.ProfileRepository
import app.cityxplore.profile.domain.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/**
 * Implementation of [ProfileRepository] using a Ktor HTTP client.
 *
 * This class manages user profile creation, updates, and retrieval from the backend API.
 * It handles race conditions, duplicate key constraints, and account conflicts gracefully.
 *
 * @property client The HTTP client for making API calls to the backend.
 * @property supabase The Supabase client for retrieving authentication information.
 */
class ProfileRepositoryImpl(
    private val client: HttpClient,
    private val supabase: SupabaseClient
) : ProfileRepository {

    private companion object {
        const val API_USERS = "https://api.cityxplore.app/api/users"
        const val API_PROFILE_ME = "$API_USERS/me"
    }

    /**
     * Creates or updates a user profile with the specified username and avatar URL.
     *
     * This method first checks if a profile exists by calling `/api/users/me`.
     * - If the profile exists, it updates it using PATCH.
     * - If the profile doesn't exist, it creates a new one using POST.
     *
     * The method handles race conditions and duplicates key constraint violations by attempting
     * to update the profile if creation fails with a conflict error.
     *
     * @param username The desired username for the profile.
     * @param avatarUrl The optional URL to the user's avatar image.
     * @return [Result] containing [Unit] on success, or exception on failure.
     * @throws IllegalStateException If an account conflict occurs with an ID mismatch.
     */
    override suspend fun createProfile(username: String, avatarUrl: String?): Result<Unit> {
        return runCatching {
            val user = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("Not authenticated")
            val email = user.email ?: throw IllegalStateException("Email not found")

            val exists = try {
                client.get(API_PROFILE_ME).status == HttpStatusCode.OK
            } catch (_: Exception) {
                false
            }

            if (exists) {
                performProfileUpdate(username, avatarUrl)
            } else {
                val request = UserCreateRequest(
                    email = email,
                    username = username,
                    avatarUrl = avatarUrl
                )
                try {
                    client.post(API_USERS) {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody(request)
                    }
                } catch (e: ClientRequestException) {
                    handleCreateProfileError(e, username, avatarUrl)
                } catch (e: ServerResponseException) {
                    handleServerError(e, username, avatarUrl)
                }
            }
        }.map { }
    }

    /**
     * Handles client request errors (4xx) during profile creation.
     *
     * @param e The client request exception.
     * @param username The username being set.
     * @param avatarUrl The avatar URL being set.
     * @throws UsernameAlreadyTakenException If the username is already taken.
     */
    private suspend fun handleCreateProfileError(
        e: ClientRequestException,
        username: String,
        avatarUrl: String?
    ) {
        if (e.response.status == HttpStatusCode.Conflict) {
            val errorBody = e.response.bodyAsText()
            // Check if it's a username conflict
            if (errorBody.contains("Username", ignoreCase = true) ||
                errorBody.contains("username", ignoreCase = true) ||
                errorBody.contains("already taken", ignoreCase = true)
            ) {
                throw UsernameAlreadyTakenException()
            }
            // Otherwise it's likely an email/profile race condition - try update
            performProfileUpdate(username, avatarUrl)
        } else {
            throw e
        }
    }

    /**
     * Handles server errors (5xx) during profile creation.
     *
     * @param e The server response exception.
     * @param username The username being set.
     * @param avatarUrl The avatar URL being set.
     * @throws UsernameAlreadyTakenException If the username constraint was violated.
     */
    private suspend fun handleServerError(
        e: ServerResponseException,
        username: String,
        avatarUrl: String?
    ) {
        val errorBody = e.response.bodyAsText()
        if (e.response.status == HttpStatusCode.InternalServerError &&
            errorBody.contains("duplicate key value violates unique constraint")
        ) {
            // Check if it's username constraint
            if (errorBody.contains("username", ignoreCase = true)) {
                throw UsernameAlreadyTakenException()
            }
            // Otherwise try update (email conflict = race condition)
            performProfileUpdate(username, avatarUrl)
        } else {
            throw e
        }
    }

    /**
     * Performs the profile update via PATCH request.
     *
     * Handles username conflict errors by extracting the error message from the response
     * and throwing a [UsernameAlreadyTakenException].
     *
     * @param username The new username for the profile.
     * @param avatarUrl The new avatar URL, or null to keep existing.
     * @throws UsernameAlreadyTakenException If the username is already taken by another user.
     */
    private suspend fun performProfileUpdate(
        username: String,
        avatarUrl: String?,
    ) {
        val updateRequest = UserUpdateRequest(
            username = username,
            avatarUrl = avatarUrl
        )
        try {
            client.patch(API_PROFILE_ME) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(updateRequest)
            }
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Conflict) {
                val errorBody = e.response.bodyAsText()
                if (errorBody.contains("Username", ignoreCase = true) ||
                    errorBody.contains("username", ignoreCase = true) ||
                    errorBody.contains("already taken", ignoreCase = true)
                ) {
                    throw UsernameAlreadyTakenException()
                }
            }
            throw e
        } catch (e: ServerResponseException) {
            // Handle backend returning 500 for constraint violations
            if (e.response.status == HttpStatusCode.InternalServerError) {
                val errorBody = e.response.bodyAsText()
                if (errorBody.contains("username", ignoreCase = true) ||
                    errorBody.contains("constraint", ignoreCase = true)
                ) {
                    throw UsernameAlreadyTakenException()
                }
            }
            throw e
        }
    }

    /**
     * Retrieves the current user's profile data from the backend.
     *
     * @return [Result] containing the [UserProfile] on success, or exception on failure.
     */
    override suspend fun getProfile(): Result<UserProfile> {
        return runCatching {
            val dto = client.get(API_PROFILE_ME).body<ProfileDto>()
            UserProfile(
                id = dto.id,
                email = dto.email,
                username = dto.username,
                avatarUrl = dto.avatarUrl,
                totalDistance = dto.totalDistance,
                totalPoisDiscovered = dto.totalPoisDiscovered,
                achievementPoints = dto.totalAchievementPoints
            )
        }
    }

    /**
     * Deletes the current user's account.
     *
     * @return [Result] containing [Unit] on success, or exception on failure.
     */
    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        // DELETE /api/users/me
        val response = client.delete(API_PROFILE_ME)
        if (!response.status.isSuccess()) {
            throw Exception("Failed to delete account: ${response.status}")
        }
    }

    /**
     * Uploads a user avatar to storage.
     *
     * @param imageBytes The raw bytes of the image to upload.
     * @return [Result] containing the public URL of the uploaded avatar on success.
     */
    override suspend fun uploadAvatar(imageBytes: ByteArray): Result<String> {
        return runCatching {
            val contentType = detectMimeType(imageBytes) ?: ContentType.Image.JPEG
            val filename = "avatar.${contentType.contentSubtype}"

            val response = client.submitFormWithBinaryData(
                url = "$API_USERS/me/avatar",
                formData = formData {
                    append("file", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, contentType.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
                }
            ).body<ProfileDto>()

            response.avatarUrl ?: throw IllegalStateException("Backend returned null avatar URL")
        }
    }

    private fun detectMimeType(bytes: ByteArray): ContentType? {
        // Simple magic bytes check
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return ContentType.Image.PNG
        }
        // JPEG: FF D8 FF
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        ) {
            return ContentType.Image.JPEG
        }
        // WEBP: RIFF ... WEBP
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()
        ) {
            return ContentType.Image.WEBP
        }
        return null
    }

    /**
     * Initiates the email change flow.
     *
     * @param newEmail The new email address.
     * @return [Result] containing Unit on success.
     */
    override suspend fun updateEmail(newEmail: String): Result<Unit> {
        return runCatching {
            supabase.auth.updateUser {
                email = newEmail
            }
        }
    }
}

/**
 * Data transfer object for creating a new user profile.
 *
 * @property email The user's email address from authentication.
 * @property username The desired username for the profile.
 * @property avatarUrl The optional URL to the user's avatar image.
 */
@Serializable
data class UserCreateRequest(
    val email: String,
    val username: String,
    val avatarUrl: String? = null
)

/**
 * Data transfer object for updating an existing user profile.
 *
 * @property username The new username for the profile.
 * @property avatarUrl The new avatar URL, or `null` to keep existing.
 */
@Serializable
data class UserUpdateRequest(
    val username: String,
    val avatarUrl: String? = null
)

/**
 * Exception thrown when attempting to use a username that is already taken.
 *
 * This exception is used to provide clear feedback to users when they try to
 * set a username that already exists in the system.
 */
class UsernameAlreadyTakenException : Exception("Username is already taken. Please choose a different one.")
