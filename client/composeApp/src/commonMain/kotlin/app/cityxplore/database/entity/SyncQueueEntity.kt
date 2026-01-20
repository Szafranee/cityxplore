package app.cityxplore.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.cityxplore.database.currentTimeMillis
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Represents the type of operation to be synced when connectivity is restored.
 */
enum class SyncOperationType {
    /** Mark a POI as discovered */
    DISCOVER_POI,

    /** Toggle favorite status on a POI */
    TOGGLE_FAVORITE,

    /** Reveal new fog of war hexagons */
    REVEAL_HEXAGONS,

    /** Sync accumulated distance */
    SYNC_DISTANCE,

    /** Send a friend invitation */
    FRIEND_INVITE,

    /** Accept a friend request */
    FRIEND_ACCEPT,

    /** Decline friend request */
    FRIEND_DECLINE,

    /** Share POI with a friend */
    SHARE_POI
}

/**
 * Room entity for queuing operations to be synced when online.
 *
 * When the user performs actions while offline (e.g. discovering a POI),
 * those operations are stored here and processed when connectivity is restored.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: String,
    val payload: String, // JSON serialized operation data
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val lastAttemptAt: Long? = null
)

/**
 * Sealed interface representing typed sync operations with their payloads.
 */
sealed interface SyncOperation {
    fun toEntity(): SyncQueueEntity

    @Serializable
    data class DiscoverPoi(val poiId: String) : SyncOperation {
        override fun toEntity() = SyncQueueEntity(
            operationType = SyncOperationType.DISCOVER_POI.name,
            payload = Json.encodeToString(this),
            createdAt = currentTimeMillis()
        )
    }

    @Serializable
    data class ToggleFavorite(val poiId: String) : SyncOperation {
        override fun toEntity() = SyncQueueEntity(
            operationType = SyncOperationType.TOGGLE_FAVORITE.name,
            payload = Json.encodeToString(this),
            createdAt = currentTimeMillis()
        )
    }

    @Serializable
    data class RevealHexagons(val hexagonIds: Set<String>) : SyncOperation {
        override fun toEntity() = SyncQueueEntity(
            operationType = SyncOperationType.REVEAL_HEXAGONS.name,
            payload = Json.encodeToString(this),
            createdAt = currentTimeMillis()
        )
    }

    @Serializable
    data class SyncDistance(val distanceMeters: Double) : SyncOperation {
        override fun toEntity() = SyncQueueEntity(
            operationType = SyncOperationType.SYNC_DISTANCE.name,
            payload = Json.encodeToString(this),
            createdAt = currentTimeMillis()
        )
    }

    @Serializable
    data class FriendInvite(val username: String) : SyncOperation {
        override fun toEntity() = SyncQueueEntity(
            operationType = SyncOperationType.FRIEND_INVITE.name,
            payload = Json.encodeToString(this),
            createdAt = currentTimeMillis()
        )
    }

    @Serializable
    data class SharePoi(val poiId: String, val friendId: String, val message: String?) : SyncOperation {
        override fun toEntity() = SyncQueueEntity(
            operationType = SyncOperationType.SHARE_POI.name,
            payload = Json.encodeToString(this),
            createdAt = currentTimeMillis()
        )
    }

    @Serializable
    data class FriendAccept(val requestId: String) : SyncOperation {
        override fun toEntity() = SyncQueueEntity(
            operationType = SyncOperationType.FRIEND_ACCEPT.name,
            payload = Json.encodeToString(this),
            createdAt = currentTimeMillis()
        )
    }

    @Serializable
    data class FriendDecline(val requestId: String) : SyncOperation {
        override fun toEntity() = SyncQueueEntity(
            operationType = SyncOperationType.FRIEND_DECLINE.name,
            payload = Json.encodeToString(this),
            createdAt = currentTimeMillis()
        )
    }

    companion object {
        /**
         * Parses a SyncQueueEntity back into a typed SyncOperation.
         */
        fun fromEntity(entity: SyncQueueEntity): SyncOperation? {
            return try {
                when (SyncOperationType.valueOf(entity.operationType)) {
                    SyncOperationType.DISCOVER_POI -> Json.decodeFromString<DiscoverPoi>(entity.payload)
                    SyncOperationType.TOGGLE_FAVORITE -> Json.decodeFromString<ToggleFavorite>(entity.payload)
                    SyncOperationType.REVEAL_HEXAGONS -> Json.decodeFromString<RevealHexagons>(entity.payload)
                    SyncOperationType.SYNC_DISTANCE -> Json.decodeFromString<SyncDistance>(entity.payload)
                    SyncOperationType.FRIEND_INVITE -> Json.decodeFromString<FriendInvite>(entity.payload)
                    SyncOperationType.SHARE_POI -> Json.decodeFromString<SharePoi>(entity.payload)
                    SyncOperationType.FRIEND_ACCEPT -> Json.decodeFromString<FriendAccept>(entity.payload)
                    SyncOperationType.FRIEND_DECLINE -> Json.decodeFromString<FriendDecline>(entity.payload)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
