package org.cityxplore.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.proxy.HibernateProxy
import java.util.UUID

@Entity
@Table(name = "achievements")
data class Achievement(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    val name: String,

    val description: String,

    val category: String? = null,

    @Column(columnDefinition = "jsonb")
    val criteria: String, // JSON string eg. {"type":"discoveries","count":10}

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    val points: Int = 0,

    @Column(name = "is_active")
    val isActive: Boolean = true
) {
    /**
     * Determines whether another object represents the same persistent Achievement by comparing non-null identifiers
     * and treating Hibernate proxy instances as their underlying persistent class.
     *
     * @param other The object to compare with this Achievement; may be a Hibernate proxy.
     * @return `true` if both objects are the same persistent entity (both have a non-null equal `id` and compatible persistent class), `false` otherwise.
     */
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as Achievement

        return id != null && id == other.id
    }

    /**
         * Compute the hash code using the underlying persistent class when this instance is a Hibernate proxy, otherwise use the runtime class's hash code.
         *
         * @return The hash code of the persistent class for proxy instances, or the hash code of this instance's runtime class otherwise.
         */
        final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    /**
     * Produce a readable string representation of the Achievement including all properties.
     *
     * @return The string containing the class simple name and the values of `id`, `name`, `description`, `category`, `criteria`, `iconUrl`, `points`, and `isActive`.
     */
    @Override
    override fun toString(): String {
        return this::class.simpleName + "(  id = $id   ,   name = $name   ,   description = $description   ,   category = $category   ,   criteria = $criteria   ,   iconUrl = $iconUrl   ,   points = $points   ,   isActive = $isActive )"
    }
}