package app.cityxplore.auth.data

import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.domain.SocialProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Discord
import io.github.jan.supabase.auth.providers.Facebook
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email

class AuthRepositoryImpl(
    client: SupabaseClient
) : AuthRepository {
    private val auth = client.auth

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

    override suspend fun signInWith(provider: SocialProvider): Result<Unit> {
        return try {
            val supabaseProvider = when (provider) {
                SocialProvider.GOOGLE -> Google
                SocialProvider.FACEBOOK -> Facebook
                SocialProvider.DISCORD -> Discord
            }
            auth.signInWith(supabaseProvider)
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
}
