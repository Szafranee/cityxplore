package org.cityxplore.backend.poi.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.hibernate.proxy.HibernateProxy
import org.locationtech.jts.geom.Point
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "points_of_interest")
data class PointOfInterest(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 200)
    var name: String,

    var description: String? = null,

    @Column(nullable = false)
    var category: String,

    @Column(columnDefinition = "GEOGRAPHY(POINT, 4326)")
    var location: Point? = null,

    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb")
    var metadata: Map<String, Any?>? = null,

    @Type(JsonType::class)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    val imageUrls: List<String>? = null,

    @Column(name = "created_at", nullable = true, updatable = false)
    @CreatedDate
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at", nullable = true)
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as PointOfInterest

        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    override fun toString(): String =
        "${'$'}{this::class.simpleName}(id=${'$'}id, name=${'$'}name, description=${'$'}description, " +
                "category=${'$'}category, location=${'$'}location, metadata=${'$'}metadata, " +
                "imageUrls=${'$'}imageUrls, createdAt=${'$'}createdAt, updatedAt=${'$'}updatedAt, isActive=${'$'}isActive)"
}
