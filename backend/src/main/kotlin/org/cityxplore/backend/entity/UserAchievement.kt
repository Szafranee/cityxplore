package org.cityxplore.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "user_achievements")
data class UserAchievement(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "achievement_id", nullable = false)
    val achievementId: UUID,

    @Column(name = "achieved_at")
    val achievedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "progress_data", columnDefinition = "jsonb")
    val progressData: String? = null
) {
    /**
     * Determines whether another object represents the same UserAchievement entity by comparing primary keys and accounting for Hibernate proxies.
     *
     * @param other The object to compare with this instance.
     * @return `true` if this instance has a non-null `id` and it equals `other`'s `id`, `false` otherwise.
     */
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as UserAchievement

        return id != null && id == other.id
    }

    /**
         * Produce a hash code based on the entity's runtime class, using the persistent class when this instance is a Hibernate proxy.
         *
         * @return An integer hash code derived from the proxy's persistent class if proxied, otherwise from the instance's runtime class.
         */
        final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    /**
     * Provides a string representation of the entity including all properties.
     *
     * @return A string containing the class name and all property values (id, userId, achievementId, achievedAt, progressData).
     */
    @Override
    override fun toString(): String {
        return this::class.simpleName + "(  id = $id   ,   userId = $userId   ,   achievementId = $achievementId   ,   achievedAt = $achievedAt   ,   progressData = $progressData )"
    }
}