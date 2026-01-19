package app.cityxplore.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.cityxplore.database.currentTimeMillis
import app.cityxplore.map.domain.PoiCategory
import app.cityxplore.map.domain.PoiModel

/**
 * Room entity for caching Point of Interest data locally.
 *
 * This entity serves as the local cache for POI data, enabling offline access
 * and reducing unnecessary API calls when the user returns to the app.
 */
@Entity(tableName = "pois")
data class PoiEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val isMajor: Boolean,
    val discovered: Boolean = false,
    val discoveryDate: Long? = null,
    val isFavorite: Boolean = false,
    val lastSyncedAt: Long
) {
    /**
     * Converts this entity to a domain model.
     */
    fun toDomain(): PoiModel = PoiModel(
        id = id,
        name = name,
        description = description,
        latitude = latitude,
        longitude = longitude,
        discovered = discovered,
        category = try {
            PoiCategory.valueOf(category)
        } catch (_: Exception) {
            PoiCategory.UNKNOWN
        },
        isMajor = isMajor,
        discoveryDate = discoveryDate,
        isFavorite = isFavorite
    )

    companion object {

        /**
         * Creates an entity from a domain model with explicit discovery info.
         * Used when refreshing from API with separate discovery data.
         */
        fun fromDomain(
            model: PoiModel,
            discovered: Boolean,
            discoveryDate: Long?,
            isFavorite: Boolean
        ): PoiEntity = PoiEntity(
            id = model.id,
            name = model.name,
            description = model.description,
            latitude = model.latitude,
            longitude = model.longitude,
            category = model.category.name,
            isMajor = model.isMajor,
            discovered = discovered,
            discoveryDate = discoveryDate,
            isFavorite = isFavorite,
            lastSyncedAt = currentTimeMillis()
        )
    }
}
