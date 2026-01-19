package app.cityxplore.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.cityxplore.database.currentTimeMillis

/**
 * Room entity for storing user's fog of war data (revealed H3 hexagons).
 *
 * This entity stores individual hexagon IDs that have been revealed by the user.
 * The fog of war system uses H3 spatial indexing for efficient hexagon-based area tracking.
 */
@Entity(tableName = "fog_of_war")
data class FogOfWarEntity(
    @PrimaryKey
    val hexagonId: String,
    val revealedAt: Long,
    val syncedToServer: Boolean = true
) {
    companion object {
        fun create(hexagonId: String, syncedToServer: Boolean = true) = FogOfWarEntity(
            hexagonId = hexagonId,
            revealedAt = currentTimeMillis(),
            syncedToServer = syncedToServer
        )
    }
}
