package app.cityxplore.map.domain

data class PoiModel(
    val id: String,
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val discovered: Boolean,
    val category: PoiCategory,
)

enum class PoiCategory { HISTORICAL, CULTURAL, FOOD, CUSTOM, UNKNOWN }

data class UserDiscovery(
    val poiId: String,
    val discoveredAt: Long,
)

fun PoiModel.toMapPoi() = MapPoi(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    discovered = discovered,
)

data class MapPoi(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val discovered: Boolean
)
