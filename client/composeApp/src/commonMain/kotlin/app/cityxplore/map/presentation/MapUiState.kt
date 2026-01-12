package app.cityxplore.map.presentation

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.core.location.Location
import app.cityxplore.map.domain.MapPoi
import app.cityxplore.profile.domain.UserProfile

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
     * @property userLocation The current location of the user, or null if not available.
     * @property isFollowingUser Whether the map camera should follow the user's location.
     * @property selectedPoi The currently selected POI for displaying details, or `null` if none selected.
     * @property newlyDiscoveredPoiIds Set of POI IDs that were just discovered (for showing notifications).
     * @property revealedHexagons Set of H3 hex indices that have been revealed (for Fog of War).
     * @property warsawHexagons Set of all H3 hex indices covering the Warsaw region (for Fog of War).
     * @property profile The current user's profile data, or null if not yet loaded.
     * @property newlyUnlockedAchievements List of achievements just unlocked (for showing celebration dialog).
     */
    data class Ready(
        val pois: List<MapPoi>,
        val userLocation: Location?,
        val isFollowingUser: Boolean,
        val selectedPoi: MapPoi?,
        val newlyDiscoveredPoiIds: Set<String>,
        val revealedHexagons: Set<String> = emptySet(),
        val warsawHexagons: Set<String> = emptySet(),
        val profile: UserProfile? = null,
        val newlyUnlockedAchievements: List<Achievement> = emptyList()
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

    /** User deselected generic POI selection (closed details) */
    data object DeselectPoi : MapAction

    /** The user granted location permission */
    data object PermissionGranted : MapAction

    /**
     * User's location was updated.
     *
     * @property location The new user location.
     */
    data class UpdateLocation(val location: Location) : MapAction

    /**
     * User dismissed the discovery notification for a POI.
     *
     * @property poiId The ID of the POI whose notification should be dismissed.
     */
    data class DismissDiscoveryNotification(val poiId: String) : MapAction

    /**
     * Toggles the favorite status of a POI.
     * @property poiId The ID of the POI to favorite/unfavorite.
     */
    data class ToggleFavorite(val poiId: String) : MapAction

    /** User requested to refresh POI data (e.g. returning from another screen) */
    data object RefreshPois : MapAction

    /**
     * User tapped "View Details" on a single discovered POI notification.
     * Selects the POI and dismisses the notification.
     *
     * @property poiId The ID of the POI to view.
     */
    data class ViewDiscoveredPoi(val poiId: String) : MapAction

    /** User dismissed all discovery notifications at once */
    data object DismissAllDiscoveryNotifications : MapAction

    /** User dismissed the achievement unlock dialog */
    data object DismissAchievementNotification : MapAction
}
