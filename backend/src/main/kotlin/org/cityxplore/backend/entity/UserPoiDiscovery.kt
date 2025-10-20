package org.cityxplore.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "user_poi_discoveries",
    uniqueConstraints = [UniqueConstraint(
        name = "uq_user_poi_discoveries_user_id_poi_id",
        columnNames = ["user_id", "poi_id"]
    )]
)
data class UserPoiDiscovery(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Column(name = "poi_id", nullable = false, updatable = false)
    val poiId: UUID,

    @Column(name = "discovered_at", nullable = false, updatable = false)
    val discoveredAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_favorite", nullable = false)
    var isFavorite: Boolean = false
) {
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as UserPoiDiscovery

        return id != null && id == other.id
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    override fun toString(): String =
        "${this::class.simpleName}(id=$id, userId=$userId, poiId=$poiId, discoveredAt=$discoveredAt, isFavorite=$isFavorite)"
}
