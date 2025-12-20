package app.cityxplore.auth.data

import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.domain.SocialProvider
import io.github.jan.supabase.SupabaseClient
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
        // If it looks like an email, return it
        if (login.contains("@")) return login

        // Otherwise, try to find email by username
        // Note: This requires public.users to be readable and contain email, or a specific RPC function.
        // Assuming we can query users table for now.
        return try {
            val user = supabase.postgrest.from("users")
                .select {
                    filter {
                        eq("username", login)
                    }
                }.decodeSingleOrNull<UserEmailDto>()
            user?.email
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun hasProfile(): Boolean {
        auth.currentUserOrNull() ?: return false
        return try {
            val response = client.get("https://api.cityxplore.app/api/users/me")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            println("AuthRepositoryImpl: hasProfile failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

@Serializable
data class UserEmailDto(val email: String)
