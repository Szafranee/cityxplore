package app.cityxplore.auth.domain

import kotlinx.coroutines.flow.Flow

enum class SocialProvider { GOOGLE, FACEBOOK, DISCORD }

interface AuthRepository {
    val authState: Flow<Boolean>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signInWith(provider: SocialProvider): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun isAuthenticated(): Boolean
}
