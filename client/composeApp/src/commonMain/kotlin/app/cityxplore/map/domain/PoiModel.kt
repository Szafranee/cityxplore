package app.cityxplore.map.domain

/**
 * Domain model representing a Point of Interest (POI).
 *
 * This is the core business logic representation of a POI, used throughout
 * the application domain and presentation layers.
 *
 * @property id The unique identifier of the POI.
 * @property name The display name of the POI.
 * @property description A detailed description of the POI, or `null` if not available.
 * @property latitude The geographic latitude coordinate.
 * @property longitude The geographic longitude coordinate.
 * @property discovered Whether the current user has discovered this POI.
 * @property category The category/type of the POI.
 */
data class PoiModel(
    val id: String,
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val discovered: Boolean,
    val category: PoiCategory,
)

/**
 * Enumeration representing POI categories in the domain layer.
 *
 * These categories determine the visual representation (icon, colour)
 * and gameplay mechanics associated with each POI type.
 */
enum class PoiCategory {
    /** Historical landmarks and monuments */
    HISTORICAL,

    /** Cultural sites (museums, theatres, galleries) */
    CULTURAL,

    /** Food and dining locations (restaurants, cafés) */
    FOOD,

    /** User-created custom POIs */
    CUSTOM,

    /** Unknown or uncategorized POIs */
    UNKNOWN
}

/**
 * Represents a user's discovery record for a POI.
 *
 * @property poiId The unique identifier of the discovered POI.
 * @property discoveredAt The timestamp when the POI was discovered (Unix epoch milliseconds).
 */
data class UserDiscovery(
    val poiId: String,
    val discoveredAt: Long,
)

/**
 * Maps a [PoiModel] to a simplified [MapPoi] representation for map rendering.
 * Strips out unnecessary fields like description and category to optimise map performance.
 *
 * @return A lightweight [MapPoi] object for map visualisation.
 */
fun PoiModel.toMapPoi() = MapPoi(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    discovered = discovered,
)

/**
 * Simplified POI representation optimised for map rendering.
 * Contains only the essential fields needed for displaying markers on the map.
 *
 * @property id The unique identifier of the POI.
 * @property name The display name of the POI.
 * @property latitude The geographic latitude coordinate.
 * @property longitude The geographic longitude coordinate.
 * @property discovered Whether the current user has discovered this POI.
 */
data class MapPoi(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val discovered: Boolean
)
