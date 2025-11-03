package org.cityxplore.backend.social.shared.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.cityxplore.backend.social.shared.dto.CustomPoiData
import org.hibernate.annotations.Type
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDateTime
import java.util.UUID

/**
 * Entity representing a shared Point of Interest between users.
 * Tracks when POIs are shared, with whom, and whether they have been viewed.
 *
 * A shared POI can reference either:
 * - An existing POI from the main points_of_interest table (via poiId)
 * - A custom POI defined inline (via poiData)
 *
 * Exactly one of poiId or poiData must be non-null.
 *
 * @property id unique identifier of the shared POI record
 * @property sharerId UUID of the user who shared the POI
 * @property recipientId UUID of the user receiving the shared POI
 * @property poiId optional UUID of an existing Point of Interest being shared
 * @property poiData optional custom POI data stored as JSONB
 * @property message optional message accompanying the shared POI
 * @property sharedAt timestamp when the POI was shared
 * @property viewedAt timestamp when the recipient viewed the shared POI, null if not yet viewed
 */
@Entity
@Table(name = "shared_pois")
data class SharedPoi(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "sharer_id", nullable = false)
    val sharerId: UUID,

    @Column(name = "recipient_id", nullable = false)
    val recipientId: UUID,

    @Column(name = "poi_id", nullable = true)
    val poiId: UUID? = null,

    @Type(JsonType::class)
    @Column(name = "poi_data", columnDefinition = "jsonb")
    val poiData: CustomPoiData? = null,

    @Column(name = "message", length = 500)
    val message: String? = null,

    @Column(name = "shared_at", nullable = false)
    val sharedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "viewed_at")
    var viewedAt: LocalDateTime? = null
) {
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as SharedPoi

        return id != null && id == other.id
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(  id = $id   ,   sharerId = $sharerId   ,   recipientId = $recipientId   ,   poiId = $poiId   ,   poiData = $poiData   ,   message = $message   ,   sharedAt = $sharedAt   ,   viewedAt = $viewedAt )"
    }
}
