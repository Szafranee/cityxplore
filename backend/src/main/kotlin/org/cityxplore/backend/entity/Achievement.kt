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

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(  id = $id   ,   name = $name   ,   description = $description   ,   category = $category   ,   criteria = $criteria   ,   iconUrl = $iconUrl   ,   points = $points   ,   isActive = $isActive )"
    }
}