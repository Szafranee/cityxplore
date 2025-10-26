package org.cityxplore.backend.achievements.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "user_achievements",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "achievement_id"])]
)
data class UserAchievement(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "achievement_id", nullable = false)
    val achievementId: UUID,

    @Column(name = "achieved_at", updatable = false)
    @CreationTimestamp
    var achievedAt: LocalDateTime? = null,

    @Type(JsonType::class)
    @Column(name = "progress_data", columnDefinition = "jsonb")
    var progressData: MutableMap<String, Any?>? = null
) {
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

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    override fun toString(): String =
        "${this::class.simpleName}(id=$id, userId=$userId, achievementId=$achievementId, " +
                "achievedAt=$achievedAt, progressData=$progressData)"
}
