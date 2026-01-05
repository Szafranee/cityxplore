package org.cityxplore.backend.fogofwar.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

/**
 * Entity representing user's fog of war progress.
 *
 * Stores the set of H3 hexagons that have been revealed by the user during exploration.
 * Uses JSONB column type for efficient storage and querying of hexagon arrays.
 *
 * @property userId Primary key - user's unique identifier
 * @property revealedHexagons Set of H3 hex index strings stored as JSONB
 * @property createdAt Timestamp when record was created
 * @property updatedAt Timestamp of the last update
 */
@Entity
@Table(name = "user_fog_of_war")
class FogOfWarEntity(
    @Id
    @Column(name = "user_id")
    var userId: UUID,

    @Type(JsonBinaryType::class)
    @Column(name = "revealed_hexagons", columnDefinition = "jsonb")
    var revealedHexagons: MutableSet<String> = mutableSetOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}
