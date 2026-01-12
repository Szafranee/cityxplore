package app.cityxplore.social.domain.repository

import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.model.RankingEntry
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    // Rankings
    fun getGlobalRanking(): Flow<List<RankingEntry>>
    suspend fun refreshGlobalRanking(): Result<Unit>

    fun getFriendsRanking(): Flow<List<RankingEntry>>
    suspend fun refreshFriendsRanking(): Result<Unit>

    // Friends Management
    fun getFriends(): Flow<List<Friendship>>
    fun getBlockedUsers(): Flow<List<Friendship>>
    suspend fun refreshFriends(): Result<Unit>
    suspend fun refreshBlockedUsers(): Result<Unit>

    fun getPendingRequests(): Flow<List<Friendship>>
    suspend fun refreshPendingRequests(): Result<Unit>

    // Actions
    suspend fun sendFriendInvite(username: String): Result<Friendship>
    suspend fun acceptFriendInvite(friendshipId: String): Result<Friendship>
    suspend fun declineFriendInvite(friendshipId: String): Result<Friendship>
    suspend fun deleteFriend(friendshipId: String): Result<Unit>
    suspend fun blockFriend(friendshipId: String): Result<Unit>
    suspend fun unblockFriend(friendshipId: String): Result<Unit>

    // Helper to resolve user details for a friendship (if not embedded)
    suspend fun getFriendProfile(userId: String): Result<RankingEntry> // Reusing RankingEntry or specific UserProfile model
}
