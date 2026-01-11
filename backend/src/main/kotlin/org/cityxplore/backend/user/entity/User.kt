package org.cityxplore.backend.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.proxy.HibernateProxy
import org.hibernate.validator.constraints.URL
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a user entity in the system.
 *
 * This class is mapped to the `users` database table and includes fields that store
 * the user's email, username, avatar URL, creation time, last activity time,
 * total distance travelled, points of interest discovered, achievement points,
 * and their active status.
 *
 * Equality and hash codes are based on the id property.
 *
 * @property id The unique identifier of the user. Auto-generated using UUID.
 * @property email The email address of the user. Must be unique and follow the email format.
 * @property username The username of the user. Must be unique and between 3 and 50 characters.
 * @property avatarUrl The URL of the user's avatar image. Optional field.
 * @property createdAt The timestamp when the user was created. Auto-generated and non-updatable.
 * @property lastActiveAt The timestamp when the user was last active. Nullable field.
 * @property totalDistance The cumulative distance travelled by the user. Defaults to 0.
 * @property totalPoisDiscovered The total number of points of interest discovered by the user. Defaults to 0.
 * @property isActive Indicates whether the user is active in the system. Defaults to true.
 * @property totalAchievementPoints The total achievement points accumulated by the user. Defaults to 0.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "users")
data class User(
    @Id
    var id: UUID? = null,

    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    @Column(nullable = false, unique = true, length = 254)
    var email: String,

    @field:NotBlank
    @field:Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @field:URL
    @Column(name = "avatar_url", length = 2048)
    var avatarUrl: String? = null,

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,

    @Column(name = "last_active_at")
    var lastActiveAt: LocalDateTime? = null,

    @Column(name = "total_distance", precision = 10, scale = 2)
    var totalDistance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_pois_discovered")
    var totalPoisDiscovered: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "total_achievement_points", nullable = false)
    var totalAchievementPoints: Int = 0,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as User

        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    override fun toString(): String =
        "${this::class.simpleName}(id=$id, avatarUrl=$avatarUrl, createdAt=$createdAt, " +
                "lastActiveAt=$lastActiveAt, totalDistance=$totalDistance, totalPoisDiscovered=$totalPoisDiscovered," +
                "totalAchievementPoints=$totalAchievementPoints, isActive=$isActive)"
}
