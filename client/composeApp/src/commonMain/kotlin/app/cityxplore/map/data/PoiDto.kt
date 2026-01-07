package app.cityxplore.map.data

import app.cityxplore.BuildConfig
import app.cityxplore.map.domain.PhotoSource
import app.cityxplore.map.domain.PoiCategory
import app.cityxplore.map.domain.PoiModel
import app.cityxplore.map.domain.PoiPhoto
import app.cityxplore.map.domain.UserDiscovery
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
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
    @SerialName("imageUrls") val imagesJson: JsonElement? = null,
    val metadata: PoiMetadataDto? = null,
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

@Serializable
data class PoiMetadataDto(
    val trivia: String? = null,
    @SerialName("opening_hours") val openingHours: OpeningHoursDto? = null,
    @SerialName("visit_duration") val visitDuration: String? = null,
    @SerialName("is_free") val isFree: Boolean? = null,
    val website: String? = null,
    val address: String? = null,
    @SerialName("build_year") val buildYear: String? = null
)

@Serializable
data class OpeningHoursDto(
    @SerialName("open_now") val openNow: Boolean? = null,
    @SerialName("weekday_text") val weekdayText: List<String> = emptyList()
)

fun PoiMetadataDto.toDomain() = app.cityxplore.map.domain.PoiMetadata(
    trivia = trivia,
    openingHours = openingHours?.toDomain(),
    visitDuration = visitDuration,
    isFree = isFree,
    website = website,
    address = address,
    buildYear = buildYear
)

fun OpeningHoursDto.toDomain() = app.cityxplore.map.domain.OpeningHours(
    openNow = openNow,
    weekdayText = weekdayText
)

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
        photos = parsePhotos(imagesJson),
        metadata = metadata?.toDomain() ?: app.cityxplore.map.domain.PoiMetadata(),
    )
}

private fun parsePhotos(element: JsonElement?): List<PoiPhoto> {
    if (element == null) return emptyList()

    return try {
        when {
            element is JsonArray -> {
                // Try parsing as array of objects first
                element.mapNotNull {
                    if (it is JsonObject) parsePoiPhoto(it)
                    else if (it.jsonPrimitive.isString) {
                        // Fallback for a simple string array: assume Unknown source or guess
                        PoiPhoto(url = it.jsonPrimitive.content, source = PhotoSource.UNKNOWN)
                    } else null
                }
            }

            element is JsonObject -> listOfNotNull(parsePoiPhoto(element))
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parsePoiPhoto(obj: JsonObject): PoiPhoto? {
    val url = obj["url"]?.jsonPrimitive?.contentOrNull
    val photoReference = obj["photo_reference"]?.jsonPrimitive?.contentOrNull
    val sourceStr = obj["source"]?.jsonPrimitive?.contentOrNull
    val author = obj["author"]?.jsonPrimitive?.contentOrNull
    val license = obj["license"]?.jsonPrimitive?.contentOrNull
    val attributions = obj["attributions"]?.jsonPrimitive?.contentOrNull

    val source = when (sourceStr) {
        "Wikimedia Commons" -> PhotoSource.WIKIMEDIA
        "Google Places" -> PhotoSource.GOOGLE_PLACES
        "User Upload" -> PhotoSource.USER
        else -> PhotoSource.UNKNOWN
    }

    val finalUrl = url ?: if (source == PhotoSource.GOOGLE_PLACES && photoReference != null) {
        // Construct Google Photo URL.
        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photo_reference=$photoReference&key=${BuildConfig.GOOGLE_MAPS_KEY}"
    } else null

    return if (finalUrl != null) {
        PoiPhoto(
            url = finalUrl,
            source = source,
            author = author,
            license = license,
            attributions = attributions
        )
    } else null
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
