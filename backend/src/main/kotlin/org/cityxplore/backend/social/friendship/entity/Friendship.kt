package org.cityxplore.backend.social.friendship.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDateTime
import java.util.UUID

/**
 * Friendship relation between two users.
 * Only one directional row is created when inviting (requester -> addressee).
 */
@Entity
@Table(name = "friendships")
data class Friendship(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "requester_id", nullable = false)
    val requesterId: UUID,

    @Column(name = "addressee_id", nullable = false)
    val addresseeId: UUID,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FriendshipStatus = FriendshipStatus.PENDING,

    @Column(name = "blocked_by")
    var blockedBy: UUID? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as Friendship

        return id != null && id == other.id
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "( id = $id, requesterId = $requesterId, addresseeId = $addresseeId, status = $status, createdAt = $createdAt, updatedAt = $updatedAt )"
    }
}
