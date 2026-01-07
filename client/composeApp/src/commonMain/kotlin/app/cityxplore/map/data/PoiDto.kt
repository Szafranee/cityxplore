package app.cityxplore.map.data

import app.cityxplore.map.domain.PoiCategory
import app.cityxplore.map.domain.PoiModel
import app.cityxplore.map.domain.UserDiscovery
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

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
 * @property isMajor Whether this is a major landmark.
 */
@Serializable
data class PoiDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("latitude") val latitudeJson: JsonElement? = null,
    @SerialName("longitude") val longitudeJson: JsonElement? = null,
    @SerialName("isDiscovered") val discovered: Boolean? = null,
    val category: PoiCategoryDto = PoiCategoryDto.UNKNOWN,
    @SerialName("isMajor") val isMajor: Boolean = false,
    @SerialName("imageUrls") val imageUrls: List<String> = emptyList(),
    val metadata: Map<String, String>? = null,
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
    @SerialName("HISTORICAL")
    HISTORICAL,

    @SerialName("CULTURAL")
    CULTURAL,

    @SerialName("NATURE")
    NATURE,

    @SerialName("FOOD")
    FOOD,

    @SerialName("SPORTS")
    SPORTS,

    @SerialName("ENTERTAINMENT")
    ENTERTAINMENT,

    @SerialName("CUSTOM")
    CUSTOM,

    @SerialName("OTHER")
    OTHER,

    UNKNOWN
}

/**
 * Maps a [PoiDto] to a domain model [app.cityxplore.map.domain.PoiModel].
 * Returns `null` if latitude or longitude is missing or invalid.
 *
 * @return A [app.cityxplore.map.domain.PoiModel] domain object, or `null` if coordinates are invalid.
 */
fun PoiDto.toDomain(): PoiModel? {
    val lat = latitude
    val lng = longitude
    if (lat == null || lng == null) return null
    return PoiModel(
        id = id,
        name = name,
        description = description,
        latitude = lat,
        longitude = lng,
        discovered = discovered ?: false,
        category = when (category) {
            PoiCategoryDto.HISTORICAL -> PoiCategory.HISTORICAL
            PoiCategoryDto.CULTURAL -> PoiCategory.CULTURAL
            PoiCategoryDto.NATURE -> PoiCategory.NATURE
            PoiCategoryDto.FOOD -> PoiCategory.FOOD
            PoiCategoryDto.SPORTS -> PoiCategory.SPORTS
            PoiCategoryDto.ENTERTAINMENT -> PoiCategory.ENTERTAINMENT
            PoiCategoryDto.CUSTOM -> PoiCategory.CUSTOM
            PoiCategoryDto.OTHER -> PoiCategory.OTHER
            PoiCategoryDto.UNKNOWN -> PoiCategory.UNKNOWN
        },
        isMajor = isMajor,
        photos = imageUrls,
        metadata = metadata ?: emptyMap(),
    )
}

/**
 * Data transfer object representing a user's POI discovery from the backend API.
 *
 * @property poiId The unique identifier of the discovered POI.
 * @property discoveredAt ISO-8601 timestamp of when the POI was discovered.
 * @property favorite Whether the user marks this discovery as a favorite.
 */
@Serializable
data class UserPoiDiscoveryDto(
    val poiId: String,
    val discoveredAt: String,
    val favorite: Boolean = false
)

/**
 * Maps a [UserPoiDiscoveryDto] to a domain model [app.cityxplore.map.domain.UserDiscovery].
 * Converts ISO-8601 timestamp to Unix epoch milliseconds.
 *
 * @return A [app.cityxplore.map.domain.UserDiscovery] domain object.
 */
fun UserPoiDiscoveryDto.toDomain(): UserDiscovery {
    val timestamp = try {
        Instant.parse(discoveredAt).toEpochMilliseconds()
    } catch (_: Exception) {
        try {
            // Fallback: try parsing as LocalDateTime (e.g. absent 'Z' or with space instead of T) and assume UTC
            val isoLike = discoveredAt.replace(' ', 'T')
            LocalDateTime.parse(isoLike)
                .toInstant(TimeZone.UTC)
                .toEpochMilliseconds()
        } catch (_: Exception) {
            0L // Fallback to 0 on parse failure
        }
    }
    return UserDiscovery(
        poiId = poiId,
        discoveredAt = timestamp
    )
}
