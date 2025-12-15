package app.cityxplore.map.data

import app.cityxplore.map.domain.PoiCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

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
    val latitude: Double?
        get() = latitudeJson?.jsonPrimitive?.doubleOrNull

    val longitude: Double?
        get() = longitudeJson?.jsonPrimitive?.doubleOrNull
}

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
