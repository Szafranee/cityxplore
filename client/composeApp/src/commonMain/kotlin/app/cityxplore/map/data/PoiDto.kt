package app.cityxplore.map.data

import app.cityxplore.map.domain.PoiCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PoiDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("is_discovered") val discovered: Boolean,
    val category: PoiCategoryDto = PoiCategoryDto.UNKNOWN,
)

@Serializable
enum class PoiCategoryDto { HISTORICAL, CULTURAL, FOOD, CUSTOM, UNKNOWN }

fun PoiDto.toDomain() = app.cityxplore.map.domain.PoiModel(
    id = id,
    name = name,
    description = description,
    latitude = latitude,
    longitude = longitude,
    discovered = discovered,
    category = when (category) {
        PoiCategoryDto.HISTORICAL -> PoiCategory.HISTORICAL
        PoiCategoryDto.CULTURAL -> PoiCategory.CULTURAL
        PoiCategoryDto.FOOD -> PoiCategory.FOOD
        PoiCategoryDto.CUSTOM -> PoiCategory.CUSTOM
        PoiCategoryDto.UNKNOWN -> PoiCategory.UNKNOWN
    }
)
