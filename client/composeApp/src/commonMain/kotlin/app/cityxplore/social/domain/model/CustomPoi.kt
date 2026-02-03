package app.cityxplore.social.domain.model

/**
 * Domain model representing a custom Point of Interest created by a user.
 * Custom POIs exist only in the context of sharing and are not part of the main POI database.
 */
data class CustomPoi(
    val name: String,
    val description: String?,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrls: List<String> = emptyList()
)
