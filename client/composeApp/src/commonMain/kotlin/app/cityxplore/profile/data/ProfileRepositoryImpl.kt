package app.cityxplore.profile.data

import app.cityxplore.profile.domain.ProfileRepository
import app.cityxplore.profile.domain.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
                client.get("https://api.cityxplore.app/api/users/me").status == HttpStatusCode.OK
            } catch (_: Exception) {
                false
            }

            if (exists) {
                val updateRequest = UserUpdateRequest(
                    username = username,
                    avatarUrl = avatarUrl
                )
                client.patch("https://api.cityxplore.app/api/users/me") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(updateRequest)
                }
            } else {
                val request = UserCreateRequest(
                    email = email,
                    username = username,
                    avatarUrl = avatarUrl
                )
                try {
                    client.post("https://api.cityxplore.app/api/users") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody(request)
                    }
                } catch (e: ClientRequestException) {
                    // Handle race condition: profile created concurrently by another request
                    if (e.response.status == HttpStatusCode.Conflict) {
                        val updateRequest = UserUpdateRequest(
                            username = username,
                            avatarUrl = avatarUrl
                        )
                        try {
                            client.patch("https://api.cityxplore.app/api/users/me") {
                                header(HttpHeaders.ContentType, ContentType.Application.Json)
                                setBody(updateRequest)
                            }
                        } catch (patchError: ClientRequestException) {
                            if (patchError.response.status == HttpStatusCode.NotFound) {
                                throw IllegalStateException(
                                    "Account conflict: Email exists but ID mismatch. Please contact support to reset your account.",
                                    patchError
                                )
                            }
                            throw patchError
                        }
                    } else {
                        throw e
                    }
                } catch (e: ServerResponseException) {
                    // Handle backend returning 500 instead of proper 409 on duplicate
                    val errorBody = e.response.bodyAsText()
                    if (e.response.status == HttpStatusCode.InternalServerError &&
                        errorBody.contains("duplicate key value violates unique constraint")
                    ) {
                        val updateRequest = UserUpdateRequest(
                            username = username,
                            avatarUrl = avatarUrl
                        )
                        try {
                            client.patch("https://api.cityxplore.app/api/users/me") {
                                header(HttpHeaders.ContentType, ContentType.Application.Json)
                                setBody(updateRequest)
                            }
                        } catch (patchError: ClientRequestException) {
                            if (patchError.response.status == HttpStatusCode.NotFound) {
                                throw IllegalStateException(
                                    "Account conflict: Email exists but ID mismatch. Please contact support to reset your account.",
                                    patchError
                                )
                            }
                            throw patchError
                        }
                    } else {
                        throw e
                    }
                }
            }
        }.map { }
    }

    /**
     * Retrieves the current user's profile data from the backend.
     *
     * @return [Result] containing the [UserProfile] on success, or exception on failure.
     */
    override suspend fun getProfile(): Result<UserProfile> {
        return runCatching {
            val dto = client.get("https://api.cityxplore.app/api/users/me").body<ProfileDto>()
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
