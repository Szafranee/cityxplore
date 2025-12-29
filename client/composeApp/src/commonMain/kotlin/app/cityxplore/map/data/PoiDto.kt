package app.cityxplore.map.data

import app.cityxplore.map.domain.PoiCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Data transfer object representing a Point of Interest from the backend API.
 *
 * This DTO handles JSON deserialization, including flexible handling of latitude/longitude
 * fields that may be received as strings or numbers from the backend.
 *
 * @property id The unique identifier of the POI.
 * @property name The name of the POI.
 * @property description The description of the POI, or `null` if not provided.
 * @property latitudeJson The raw JSON element for latitude (may be string or number).
 * @property longitudeJson The raw JSON element for longitude (may be string or number).
 * @property discovered Whether the current user has discovered this POI.
 * @property category The category of the POI.
 */
@Serializable
data class PoiDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("latitude") val latitudeJson: JsonElement? = null,
    @SerialName("longitude") val longitudeJson: JsonElement? = null,
    @SerialName("is_discovered") val discovered: Boolean? = null,
    val category: PoiCategoryDto = PoiCategoryDto.UNKNOWN,
) {
    /**
     * Parses the latitude from JSON, handling both string and numeric formats.
     */
    val latitude: Double?
        get() = latitudeJson?.jsonPrimitive?.doubleOrNull

    /**
     * Parses the longitude from JSON, handling both string and numeric formats.
     */
    val longitude: Double?
        get() = longitudeJson?.jsonPrimitive?.doubleOrNull
}

/**
 * Enumeration representing POI categories as received from the backend.
 *
 * @see app.cityxplore.map.domain.PoiCategory
 */
@Serializable
enum class PoiCategoryDto {
    @SerialName("Historical")
    HISTORICAL,

    @SerialName("Cultural")
    CULTURAL,

    @SerialName("Food")
    FOOD,

    @SerialName("Custom")
    CUSTOM,

    @SerialName("Tested")
    TESTED,

    @SerialName("Nature")
    NATURE,

    UNKNOWN
}

/**
 * Maps a [PoiDto] to a domain model [app.cityxplore.map.domain.PoiModel].
 * Returns `null` if latitude or longitude is missing or invalid.
 *
 * @return A [app.cityxplore.map.domain.PoiModel] domain object, or `null` if coordinates are invalid.
 */
fun PoiDto.toDomain(): app.cityxplore.map.domain.PoiModel? {
    val lat = latitude
    val lng = longitude
    if (lat == null || lng == null) return null
    return app.cityxplore.map.domain.PoiModel(
        id = id,
        name = name,
        description = description,
        latitude = lat,
        longitude = lng,
        discovered = discovered ?: false,
        category = when (category) {
            PoiCategoryDto.HISTORICAL -> PoiCategory.HISTORICAL
            PoiCategoryDto.CULTURAL -> PoiCategory.CULTURAL
            PoiCategoryDto.FOOD -> PoiCategory.FOOD
            PoiCategoryDto.CUSTOM -> PoiCategory.CUSTOM
            PoiCategoryDto.TESTED -> PoiCategory.UNKNOWN
            PoiCategoryDto.NATURE -> PoiCategory.UNKNOWN
            PoiCategoryDto.UNKNOWN -> PoiCategory.UNKNOWN
        }
    )
}
