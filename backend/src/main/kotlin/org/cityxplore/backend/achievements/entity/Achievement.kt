package org.cityxplore.backend.achievements.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import org.hibernate.annotations.Type
import org.hibernate.proxy.HibernateProxy
import org.hibernate.validator.constraints.URL
import java.util.UUID

@Entity
@Table(name = "achievements")
data class Achievement(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @field:NotBlank
    @Column(nullable = false, length = 120)
    val name: String,

    @field:NotBlank
    @Column(nullable = false, length = 500)
    val description: String,

    @Column(length = 50)
    val category: String? = null,

    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb", nullable = false)
    val criteria: Map<String, Any?>, // e.g. {"type":"discoveries","count":10}

    @field:URL
    @Column(name = "icon_url", length = 2048)
    val iconUrl: String? = null,

    @field:PositiveOrZero
    @Column(nullable = false)
    val points: Int = 0,

    @Column(name = "is_active", nullable = false)
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

    override fun toString(): String =
        "${this::class.simpleName}(id=$id, name=$name, description=$description, " +
                "category=$category, criteria=$criteria, iconUrl=$iconUrl, points=$points, isActive=$isActive)"
}
