package app.cityxplore.auth.domain

enum class SocialProvider { GOOGLE, FACEBOOK, DISCORD }

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signInWith(provider: SocialProvider): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun isAuthenticated(): Boolean
}
