package app.cityxplore.social.data.repository

import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.social.data.remote.dto.FriendshipDto
import app.cityxplore.social.data.remote.dto.FriendshipRequestDto
import app.cityxplore.social.data.remote.dto.RankingEntryDto
import app.cityxplore.social.domain.exception.CannotInviteSelfException
import app.cityxplore.social.domain.exception.UserNotFoundException
import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.model.FriendshipStatus
import app.cityxplore.social.domain.model.RankingEntry
import app.cityxplore.social.domain.repository.SocialRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Implementation of SocialRepository using Ktor for remote API calls.
 * This class handles data fetching/posting for rankings and friends management.
 */
class SocialRepositoryImpl(
    private val client: HttpClient,
    private val authRepository: AuthRepository
) : SocialRepository {

    // Internal flows to hold the cached state
    private val _globalRanking = MutableStateFlow<List<RankingEntry>>(emptyList())
    private val _friendsRanking = MutableStateFlow<List<RankingEntry>>(emptyList())
    private val _friends = MutableStateFlow<List<Friendship>>(emptyList())
    private val _pendingRequests = MutableStateFlow<List<Friendship>>(emptyList())
    private val _blockedUsers = MutableStateFlow<List<Friendship>>(emptyList())

    override fun getGlobalRanking(): Flow<List<RankingEntry>> = _globalRanking.asStateFlow()

    override suspend fun refreshGlobalRanking(): Result<Unit> = runCatching {
        val dtos = client.get("https://api.cityxplore.app/api/rankings/global").body<List<RankingEntryDto>>()
        _globalRanking.update { dtos.map { it.toDomain() } }
    }

    override fun getFriendsRanking(): Flow<List<RankingEntry>> = _friendsRanking.asStateFlow()

    override fun getBlockedUsers(): Flow<List<Friendship>> = _blockedUsers.asStateFlow()

    override suspend fun refreshBlockedUsers(): Result<Unit> = runCatching {
        val currentUserId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        val dtos = client.get("https://api.cityxplore.app/api/friends/blocked").body<List<FriendshipDto>>()

        // Fetch global ranking once to reuse for all profile lookups
        val rankingMap = fetchGlobalRankingMap()

        // Enrich blocked users with profile information
        val enriched = dtos.map { dto ->
            val domain = dto.toDomain()
            try {
                val otherUserId = if (dto.requesterId == currentUserId) dto.addresseeId else dto.requesterId
                val profile = rankingMap[otherUserId]
                profile?.let {
                    domain.copy(
                        otherUserId = otherUserId,
                        otherUserName = it.username,
                        otherUserAvatar = it.avatarUrl
                    )
                } ?: domain.copy(otherUserId = otherUserId)
            } catch (_: Exception) {
                domain
            }
        }
        _blockedUsers.update { enriched }
    }

    override suspend fun refreshFriendsRanking(): Result<Unit> = runCatching {
        val dtos = client.get("https://api.cityxplore.app/api/rankings/friends").body<List<RankingEntryDto>>()
        _friendsRanking.update { dtos.map { it.toDomain() } }
    }

    override fun getFriends(): Flow<List<Friendship>> = _friends.asStateFlow()

    override suspend fun refreshFriends(): Result<Unit> = runCatching {
        val currentUserId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")

        // Fetch the raw friendship list and global ranking once
        val dtos = client.get("https://api.cityxplore.app/api/friends").body<List<FriendshipDto>>()
        val rankingMap = fetchGlobalRankingMap()

        // Enrich using the cached ranking map
        val enriched = dtos.map { dto ->
            val domain = dto.toDomain()
            try {
                val otherUserId = if (dto.requesterId == currentUserId) dto.addresseeId else dto.requesterId
                val profile = rankingMap[otherUserId]
                profile?.let {
                    domain.copy(
                        otherUserId = otherUserId,
                        otherUserName = it.username,
                        otherUserAvatar = it.avatarUrl
                    )
                } ?: domain.copy(otherUserId = otherUserId)
            } catch (_: Exception) {
                domain
            }
        }
        _friends.update { enriched }
    }

    override fun getPendingRequests(): Flow<List<Friendship>> = _pendingRequests.asStateFlow()

    override suspend fun refreshPendingRequests(): Result<Unit> = runCatching {
        val dtos = client.get("https://api.cityxplore.app/api/friends/pending").body<List<FriendshipDto>>()

        // Fetch global ranking once for all profile lookups
        val rankingMap = fetchGlobalRankingMap()

        // Enrich pending requests with requester profile information
        val enriched = dtos.map { dto ->
            val domain = dto.toDomain()
            try {
                val profile = rankingMap[dto.requesterId]
                profile?.let {
                    domain.copy(otherUserName = it.username, otherUserAvatar = it.avatarUrl)
                } ?: domain
            } catch (_: Exception) {
                domain
            }
        }
        _pendingRequests.update { enriched }
    }

    override suspend fun sendFriendInvite(username: String): Result<Friendship> = runCatching {
        // DESIGN DECISION: Since the backend currently lacks a dedicated "search user by username"
        // endpoint, we leverage the cached Global Ranking as a directory to resolve the
        // username to a userId. This is efficient for small-to-medium user bases but
        // will need refactoring to a proper server-side search as the user base scales.
        val ranking = client.get("https://api.cityxplore.app/api/rankings/global").body<List<RankingEntryDto>>()
        val target = ranking.find { it.username.equals(username, ignoreCase = true) }
            ?: throw UserNotFoundException(username)

        // Check if user is trying to send a friend request to themselves
        val currentUserId = authRepository.getCurrentUserId()
        if (target.userId == currentUserId) {
            throw CannotInviteSelfException()
        }

        val response = client.post("https://api.cityxplore.app/api/friends/invite") {
            contentType(ContentType.Application.Json)
            setBody(FriendshipRequestDto(addresseeId = target.userId))
        }.body<FriendshipDto>()

        response.toDomain()
    }

    override suspend fun acceptFriendInvite(friendshipId: String): Result<Friendship> = runCatching {
        val response = client.post("https://api.cityxplore.app/api/friends/$friendshipId/accept").body<FriendshipDto>()
        response.toDomain()
    }

    override suspend fun declineFriendInvite(friendshipId: String): Result<Friendship> = runCatching {
        val response = client.post("https://api.cityxplore.app/api/friends/$friendshipId/decline").body<FriendshipDto>()
        response.toDomain()
    }

    override suspend fun deleteFriend(friendshipId: String): Result<Unit> = runCatching {
        val response = client.delete("https://api.cityxplore.app/api/friends/$friendshipId")
        if (response.status.value !in 200..299) {
            throw Exception("Failed to delete friend")
        }
    }

    override suspend fun blockFriend(friendshipId: String): Result<Unit> = runCatching {
        // POST /api/friends/{id}/block is assumed
        val response = client.post("https://api.cityxplore.app/api/friends/$friendshipId/block")
        if (response.status.value !in 200..299) {
            throw Exception("Failed to block friend")
        }
    }

    override suspend fun unblockFriend(friendshipId: String): Result<Unit> = runCatching {
        val response = client.post("https://api.cityxplore.app/api/friends/$friendshipId/unblock")
        if (response.status.value !in 200..299) {
            throw Exception("Failed to unblock friend")
        }
    }

    override suspend fun getFriendProfile(userId: String): Result<RankingEntry> = runCatching {
        val response = client.get("https://api.cityxplore.app/api/rankings/global").body<List<RankingEntryDto>>()
        val found = response.firstOrNull { it.userId == userId }
        val dto = found ?: client.get("https://api.cityxplore.app/api/users/$userId").body<UserResponseDto>().let {
            RankingEntryDto(
                userId = it.id ?: userId,
                username = it.username,
                avatarUrl = it.avatarUrl,
                score = 0.0,
                totalPoisDiscovered = it.totalPoisDiscovered,
                totalDistance = it.totalDistance,
                totalAchievementPoints = 0,
                rank = 0
            )
        }
        dto.toDomain()
    }

    override suspend fun checkIfBlocked(otherUserId: String): Result<Boolean> = runCatching {
        val response = client.get("https://api.cityxplore.app/api/friends/blocked/$otherUserId")
            .body<Map<String, Boolean>>()
        response["blocked"] ?: false
    }

    /**
     * Fetches the global ranking once and returns it as a map for efficient lookups.
     * This prevents N separate API calls when enriching multiple friendships.
     */
    private suspend fun fetchGlobalRankingMap(): Map<String, RankingEntry> {
        val response = client.get("https://api.cityxplore.app/api/rankings/global").body<List<RankingEntryDto>>()
        return response.associate { it.userId to it.toDomain() }
    }

    // Mappers
    private fun FriendshipDto.toDomain(): Friendship {
        // Robust date parsing is needed because backend timestamp formats
        // might vary (ISO-8601 vs Local) or contain unexpected zones.
        // We try standard LocalDateTime first, then fallback to Instant.
        val created = try {
            LocalDateTime.parse(createdAt)
        } catch (_: Exception) {
            try {
                Instant.parse(createdAt).toLocalDateTime(TimeZone.UTC)
            } catch (_: Exception) {
                LocalDateTime(2024, 1, 1, 0, 0)
            }
        }
        val updated = try {
            LocalDateTime.parse(updatedAt)
        } catch (_: Exception) {
            try {
                Instant.parse(updatedAt).toLocalDateTime(TimeZone.UTC)
            } catch (_: Exception) {
                LocalDateTime(2024, 1, 1, 0, 0)
            }
        }

        return Friendship(
            id = id,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = enumValues<FriendshipStatus>().firstOrNull {
                it.name.equals(status, ignoreCase = true)
            } ?: FriendshipStatus.UNKNOWN,
            blockedBy = blockedBy,
            createdAt = created,
            updatedAt = updated,
            otherUserAvatar = null,
            otherUserName = null
        )
    }

    private fun RankingEntryDto.toDomain() = RankingEntry(
        userId = userId,
        username = username,
        avatarUrl = avatarUrl,
        score = score,
        totalPoisDiscovered = totalPoisDiscovered,
        totalDistance = totalDistance,
        totalAchievementPoints = totalAchievementPoints,
        rank = rank
    )

    override fun clearCache() {
        _globalRanking.value = emptyList()
        _friendsRanking.value = emptyList()
        _friends.value = emptyList()
        _pendingRequests.value = emptyList()
        _blockedUsers.value = emptyList()
    }
}

// Minimal UserResponseDto for getFriendProfile
@kotlinx.serialization.Serializable
data class UserResponseDto(
    val id: String?,
    val username: String,
    val avatarUrl: String?,
    val totalDistance: Double,
    val totalPoisDiscovered: Int
)
