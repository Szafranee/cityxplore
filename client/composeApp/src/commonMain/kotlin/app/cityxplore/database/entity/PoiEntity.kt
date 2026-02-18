package app.cityxplore.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.cityxplore.database.currentTimeMillis
import app.cityxplore.map.domain.PhotoSource
import app.cityxplore.map.domain.PoiCategory
import app.cityxplore.map.domain.PoiMetadata
import app.cityxplore.map.domain.PoiModel
import app.cityxplore.map.domain.PoiPhoto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON serializer configured for lenient parsing.
 */
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * Serializable wrapper for PoiPhoto to store in Room as JSON.
 */
@Serializable
data class PoiPhotoEntity(
    val url: String,
    val source: String,
    val author: String? = null,
    val license: String? = null,
    val attributions: String? = null
) {
    fun toDomain(): PoiPhoto = PoiPhoto(
        url = url,
        source = try {
            PhotoSource.valueOf(source)
        } catch (_: Exception) {
            PhotoSource.UNKNOWN
        },
        author = author,
        license = license,
        attributions = attributions
    )

    companion object {
        fun fromDomain(photo: PoiPhoto): PoiPhotoEntity = PoiPhotoEntity(
            url = photo.url,
            source = photo.source.name,
            author = photo.author,
            license = photo.license,
            attributions = photo.attributions
        )
    }
}

/**
 * Serializable wrapper for PoiMetadata to store in Room as JSON.
 * Uses snake_case field names to match the backend API format.
 */
@Serializable
data class PoiMetadataEntity(
    val trivia: String? = null,
    @SerialName("opening_hours") val openingHours: List<String>? = null,
    @SerialName("visit_duration") val visitDuration: String? = null,
    @SerialName("is_free") val isFree: Boolean? = null,
    val website: String? = null,
    val address: String? = null,
    @SerialName("build_year") val buildYear: String? = null
) {
    fun toDomain(): PoiMetadata = PoiMetadata(
        trivia = trivia,
        openingHours = openingHours,
        visitDuration = visitDuration,
        isFree = isFree,
        website = website,
        address = address,
        buildYear = buildYear
    )

    companion object {
        fun fromDomain(metadata: PoiMetadata): PoiMetadataEntity = PoiMetadataEntity(
            trivia = metadata.trivia,
            openingHours = metadata.openingHours,
            visitDuration = metadata.visitDuration,
            isFree = metadata.isFree,
            website = metadata.website,
            address = metadata.address,
            buildYear = metadata.buildYear
        )
    }
}

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
    val photosJson: String = "[]",
    val metadataJson: String = "{}",
    val lastSyncedAt: Long
) {
    /**
     * Converts this entity to a domain model.
     */
    fun toDomain(): PoiModel {
        val photos = try {
            json.decodeFromString<List<PoiPhotoEntity>>(photosJson).map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }

        val metadata = try {
            json.decodeFromString<PoiMetadataEntity>(metadataJson).toDomain()
        } catch (_: Exception) {
            PoiMetadata()
        }

        return PoiModel(
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
            photos = photos,
            metadata = metadata,
            discoveryDate = discoveryDate,
            isFavorite = isFavorite
        )
    }

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
        ): PoiEntity {
            val photosJson = try {
                json.encodeToString(model.photos.map { PoiPhotoEntity.fromDomain(it) })
            } catch (_: Exception) {
                "[]"
            }

            val metadataJson = try {
                json.encodeToString(PoiMetadataEntity.fromDomain(model.metadata))
            } catch (_: Exception) {
                "{}"
            }

            return PoiEntity(
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
                photosJson = photosJson,
                metadataJson = metadataJson,
                lastSyncedAt = currentTimeMillis()
            )
        }
    }
}
