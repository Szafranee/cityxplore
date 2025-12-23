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

class ProfileRepositoryImpl(
    private val client: HttpClient,
    private val supabase: SupabaseClient
) : ProfileRepository {

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

    override suspend fun getProfile(): Result<UserProfile> {
        return runCatching {
            client.get("https://api.cityxplore.app/api/users/me").body()
        }
    }
}

@Serializable
data class UserCreateRequest(
    val email: String,
    val username: String,
    val avatarUrl: String? = null
)

@Serializable
data class UserUpdateRequest(
    val username: String,
    val avatarUrl: String? = null
)
