package app.cityxplore.social.presentation.sharedpois

import app.cityxplore.social.domain.model.CustomPoi
import app.cityxplore.social.domain.model.SharedPoi

/**
 * Sealed interface representing the UI state for the Shared POIs screen.
 */
sealed interface SharedPoisUiState {
    data object Loading : SharedPoisUiState

    data class Content(
        val receivedPois: List<SharedPoi>,
        val sentPois: List<SharedPoi>,
        val unviewedCount: Int
    ) : SharedPoisUiState

    data class Error(val message: String) : SharedPoisUiState
}

/**
 * UI events emitted by the SharedPoisViewModel.
 */
sealed interface SharedPoisUiEvent {
    data class ShowMessage(val message: String) : SharedPoisUiEvent
    data class NavigateToPoiOnMap(val latitude: Double, val longitude: Double) : SharedPoisUiEvent
    data object ShareSuccess : SharedPoisUiEvent
}

/**
 * State for the Create Custom POI dialogue.
 */
data class CreateCustomPoiState(
    val name: String = "",
    val description: String = "",
    val category: String = "viewpoint",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val isLocationPickerVisible: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
                name.length <= 200 &&
                (description.isEmpty() || description.length <= 1000) &&
                category.isNotBlank() &&
                latitude != null &&
                longitude != null

    fun toCustomPoi(): CustomPoi? {
        if (!isValid) return null
        return CustomPoi(
            name = name.trim(),
            description = description.trim().ifEmpty { null },
            category = category,
            latitude = latitude!!,
            longitude = longitude!!,
            imageUrls = imageUrls
        )
    }
}

/**
 * Available categories for custom POIs.
 */
val customPoiCategories = listOf(
    "viewpoint" to "Viewpoint",
    "restaurant" to "Restaurant",
    "cafe" to "Café",
    "bar" to "Bar",
    "park" to "Park",
    "beach" to "Beach",
    "museum" to "Museum",
    "monument" to "Monument",
    "shopping" to "Shopping",
    "entertainment" to "Entertainment",
    "nature" to "Nature",
    "other" to "Other"
)
