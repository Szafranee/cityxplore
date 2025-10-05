package org.cityxplore.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import org.springframework.data.geo.Point
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "points_of_interest")
data class PointOfInterest(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false, length = 200)
    val name: String,

    val description: String? = null,

    @Column(nullable = false)
    val category: String,

    @Column(columnDefinition = "GEOGRAPHY(POINT, 4326)")
    val location: Point? = null,

    @Column(columnDefinition = "jsonb")
    val metadata: String? = null,

    @Column(name = "image_urls", columnDefinition = "jsonb")
    val imageUrls: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = LocalDateTime.now(),

    @Column(name = "is_active")
    val isActive: Boolean = true
) {
    final override fun equals(other: Any?): Boolean {
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

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(  id = $id   ,   name = $name   ,   description = $description   ,   category = $category   ,   location = $location   ,   metadata = $metadata   ,   imageUrls = $imageUrls   ,   createdAt = $createdAt   ,   updatedAt = $updatedAt   ,   isActive = $isActive )"
    }
}