package app.cityxplore.auth.data

import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.domain.SocialProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Discord
import io.github.jan.supabase.auth.providers.Facebook
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

class AuthRepositoryImpl(
    private val supabase: SupabaseClient,
    private val client: HttpClient
) : AuthRepository {
    private val auth = supabase.auth

    override val authState: Flow<Boolean> = auth.sessionStatus.map {
        it is SessionStatus.Authenticated
    }

    override suspend fun signInWith(provider: SocialProvider): Result<Unit> {
        return try {
            val redirectUrl = "app.cityxplore://login"
            when (provider) {
                SocialProvider.GOOGLE -> auth.signInWith(Google, redirectUrl)
                SocialProvider.FACEBOOK -> auth.signInWith(Facebook, redirectUrl)
                SocialProvider.DISCORD -> auth.signInWith(Discord, redirectUrl)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    override suspend fun isAuthenticated(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    override suspend fun resolveEmail(login: String): String? {
        if (login.contains("@")) return login

        // Query users table to find email by username
        return try {
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
    }

    override suspend fun hasProfile(): Boolean {
        auth.currentUserOrNull() ?: return false
        return try {
            val response = client.get("https://api.cityxplore.app/api/users/me")
            response.status == HttpStatusCode.OK
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return try {
            auth.resendEmail(OtpType.Email.SIGNUP, email)
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(Exception("Failed to resend verification email"))
        }
    }

    override suspend fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
}

@Serializable
data class UserEmailDto(val email: String)
