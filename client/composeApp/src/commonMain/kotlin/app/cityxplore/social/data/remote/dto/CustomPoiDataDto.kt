package app.cityxplore.social.data.remote.dto

import app.cityxplore.social.domain.model.CustomPoi
import kotlinx.serialization.Serializable

/**
 * DTO representing custom Point of Interest data for sharing.
 * Maps to backend's CustomPoiData.
 */
@Serializable
data class CustomPoiDataDto(
    val name: String,
    val description: String? = null,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrls: List<String>? = null
) {
    /**
     * Maps this DTO to a domain model.
     */
    fun toDomain(): CustomPoi = CustomPoi(
        name = name,
        description = description,
        category = category,
        latitude = latitude,
        longitude = longitude,
        imageUrls = imageUrls ?: emptyList()
    )

    companion object {
        /**
         * Creates a DTO from a domain model.
         */
        fun fromDomain(customPoi: CustomPoi): CustomPoiDataDto = CustomPoiDataDto(
            name = customPoi.name,
            description = customPoi.description,
            category = customPoi.category,
            latitude = customPoi.latitude,
            longitude = customPoi.longitude,
            imageUrls = customPoi.imageUrls.ifEmpty { null }
        )
    }
}
