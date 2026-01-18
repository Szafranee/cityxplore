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
 * Steps in the Create Custom POI wizard.
 */
enum class CreatePoiStep {
    /** Step 1: Name, description, category */
    BASIC_INFO,

    /** Step 2: Location and photo */
    LOCATION_PHOTO,

    /** Step 3: Select a friend to share with */
    SELECT_FRIEND
}

/**
 * State for the Create Custom POI wizard (multistep).
 */
data class CreateCustomPoiState(
    val currentStep: CreatePoiStep = CreatePoiStep.BASIC_INFO,
    val name: String = "",
    val description: String = "",
    val category: String = "landmark",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageBytes: ByteArray? = null,
    val isLocationPickerVisible: Boolean = false,
    val isUploading: Boolean = false
) {
    /** Step 1 is valid when the name and category are filled */
    val isStep1Valid: Boolean
        get() = name.isNotBlank() &&
                name.length <= 200 &&
                (description.isEmpty() || description.length <= 1000) &&
                category.isNotBlank()

    /** Step 2 is valid when location is set (a photo is optional) */
    val isStep2Valid: Boolean
        get() = latitude != null && longitude != null

    /** Overall validity for creating the POI */
    val isValid: Boolean
        get() = isStep1Valid && isStep2Valid

    fun toCustomPoi(uploadedImageUrl: String? = null): CustomPoi? {
        if (!isValid) return null
        return CustomPoi(
            name = name.trim(),
            description = description.trim().ifEmpty { null },
            category = category,
            latitude = latitude!!,
            longitude = longitude!!,
            imageUrls = if (uploadedImageUrl != null) listOf(uploadedImageUrl) else emptyList()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CreateCustomPoiState
        return currentStep == other.currentStep &&
                name == other.name &&
                description == other.description &&
                category == other.category &&
                latitude == other.latitude &&
                longitude == other.longitude &&
                imageBytes.contentEquals(other.imageBytes) &&
                isLocationPickerVisible == other.isLocationPickerVisible &&
                isUploading == other.isUploading
    }

    override fun hashCode(): Int {
        var result = currentStep.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + isLocationPickerVisible.hashCode()
        result = 31 * result + isUploading.hashCode()
        return result
    }
}

/**
 * Available categories for custom POIs - matching PoiCategory enum values.
 * The first value (key) must match the PoiCategory enum name for proper icon/color mapping.
 */
val customPoiCategories = listOf(
    "HISTORICAL" to "Historical",
    "CULTURAL" to "Cultural",
    "NATURE" to "Nature",
    "FOOD" to "Food & Dining",
    "SPORTS" to "Sports",
    "ENTERTAINMENT" to "Entertainment",
    "OTHER" to "Other"
)
