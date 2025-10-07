package org.cityxplore.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false, unique = true, length = 50)
    val username: String,

    @Column(name = "avatar_url")
    val avatarUrl: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = LocalDateTime.now(),

    @Column(name = "last_active_at")
    val lastActiveAt: LocalDateTime? = null,

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

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(  id = $id   ,   email = $email   ,   username = $username   ,   avatarUrl = $avatarUrl   ,   createdAt = $createdAt   ,   lastActiveAt = $lastActiveAt   ,   totalDistance = $totalDistance   ,   totalPoisDiscovered = $totalPoisDiscovered )"
    }
}