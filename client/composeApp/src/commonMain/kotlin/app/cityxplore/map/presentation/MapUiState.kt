package app.cityxplore.map.presentation

import app.cityxplore.map.domain.MapPoi

/**
 * Sealed interface representing the state of the map screen.
 *
 * This state machine controls the map UI rendering and determines
 * whether POIs, loading indicators, or error messages should be displayed.
 */
sealed interface MapUiState {
    /** Initial loading state while fetching POI data */
    data object Loading : MapUiState

    /**
     * Map is ready with POI data loaded.
     *
     * @property pois The list of POIs to display on the map.
     * @property isFollowingUser Whether the map camera should follow the user's location.
     * @property selectedPoi The currently selected POI for displaying details, or `null` if none selected.
     */
    data class Ready(
        val pois: List<MapPoi>,
        val isFollowingUser: Boolean,
        val selectedPoi: MapPoi?
    ) : MapUiState

    /**
     * An error occurred while loading map data.
     *
     * @property message The error message to display to the user.
     */
    data class Error(val message: String) : MapUiState
}

/**
 * Sealed interface representing user actions on the map screen.
 *
 * These actions are dispatched from the UI layer to the ViewModel
 * to trigger state changes and business logic.
 */
sealed interface MapAction {
    /** User requested to refresh POI data */
    data object Refresh : MapAction

    /** User toggled the follow-user-location mode */
    data object ToggleFollowUser : MapAction

    /**
     * User selected a POI on the map.
     *
     * @property poiId The unique identifier of the selected POI.
     */
    data class SelectPoi(val poiId: String) : MapAction

    /** The user granted location permission */
    data object PermissionGranted : MapAction
}
