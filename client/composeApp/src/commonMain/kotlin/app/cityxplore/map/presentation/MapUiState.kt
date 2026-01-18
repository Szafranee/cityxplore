package app.cityxplore.map.presentation

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.core.location.Location
import app.cityxplore.map.domain.MapPoi
import app.cityxplore.profile.domain.UserProfile
import app.cityxplore.social.domain.model.SharedPoi

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
     * @property newlyUnlockedAchievements List of achievements just unlocked (for showing celebration dialogue).
     * @property newLevel The new level if the user just levelled up, null otherwise.
     * @property sharedPois List of POIs shared by friends to display on the map.
     * @property selectedSharedPoi The currently selected shared POI for displaying details.
     * @property targetCameraLocation Location to centre the camera on (set externally, e.g. from Journal).
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
        val newlyUnlockedAchievements: List<Achievement> = emptyList(),
        val newLevel: Int? = null,
        val sharedPois: List<SharedPoi> = emptyList(),
        val selectedSharedPoi: SharedPoi? = null,
        val newlyDiscoveredSharedPoiIds: Set<String> = emptySet(),
        val targetCameraLocation: Location? = null
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

    /** User dismissed the achievement unlock dialogue */
    data object DismissAchievementNotification : MapAction

    /** User dismissed the level up dialogue */
    data object DismissLevelUpDialog : MapAction

    /**
     * User tapped on a shared POI marker.
     *
     * @property sharedPoiId The ID of the shared POI.
     */
    data class SelectSharedPoi(val sharedPoiId: String) : MapAction

    /**
     * Center the map on specific coordinates.
     * Used when navigating to a POI from the Journal or Shared POIs list.
     *
     * @property latitude The latitude to centre on.
     * @property longitude The longitude to centre on.
     */
    data class CenterOnLocation(val latitude: Double, val longitude: Double) : MapAction

    /**
     * User dismissed the discovery notification for a Shared POI.
     *
     * @property sharedPoiId The ID of the Shared POI whose notification should be dismissed.
     */
    data class DismissSharedDiscoveryNotification(val sharedPoiId: String) : MapAction

    /**
     * User tapped "View Details" on a single discovered Shared POI notification.
     * Selects the Shared POI and dismisses the notification.
     *
     * @property sharedPoiId The ID of the Shared POI to view.
     */
    data class ViewDiscoveredSharedPoi(val sharedPoiId: String) : MapAction

    /** User dismissed all Shared POI discovery notifications at once */
    data object DismissAllSharedDiscoveryNotifications : MapAction
}
