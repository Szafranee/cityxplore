package org.cityxplore.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
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

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    @Column(nullable = false, unique = true, length = 254)
    val email: String,

    @field:NotBlank
    @field:Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    val username: String,

    @field:URL
    @Column(name = "avatar_url", length = 2048)
    val avatarUrl: String? = null,

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,

    @Column(name = "last_active_at")
    var lastActiveAt: LocalDateTime? = null,

    @Column(name = "total_distance", precision = 10, scale = 2)
    val totalDistance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_pois_discovered")
    val totalPoisDiscovered: Int = 0
) {
    final override fun equals(other: Any?): Boolean {
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

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    override fun toString(): String =
        "${this::class.simpleName}(id=$id, avatarUrl=$avatarUrl, createdAt=$createdAt, " +
                "lastActiveAt=$lastActiveAt, totalDistance=$totalDistance, totalPoisDiscovered=$totalPoisDiscovered)"
}
