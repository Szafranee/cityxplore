package app.cityxplore.map.presentation

import app.cityxplore.core.cityXploreDispatchers
import app.cityxplore.core.location.Location
import app.cityxplore.core.location.LocationService
import app.cityxplore.core.utils.calculateDistance
import app.cityxplore.map.data.PoiRepository
import app.cityxplore.map.domain.PoiModel
import app.cityxplore.map.domain.toMapPoi
import app.cityxplore.map.presentation.MapViewModel.Companion.DISCOVERY_THRESHOLD_METERS
import app.cityxplore.platform.CityXploreBaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the map screen state and POI discovery logic.
 *
 * This ViewModel handles:
 * - Fetching POI data from the backend
 * - Observing user location to trigger automatic POI discovery
 * - Managing map camera follow mode
 * - Handling POI selection for detail display
 *
 * POIs are automatically discovered when the user is within [DISCOVERY_THRESHOLD_METERS]
 * of an undiscovered POI. The discovery request is sent to the backend, and the POI list
 * is refreshed upon successful discovery.
 *
 * @property repository The repository for POI data and discovery operations.
 * @property locationService The service providing user location updates.
 */
class MapViewModel(
    private val repository: PoiRepository,
    private val locationService: LocationService
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<MapUiState>(MapUiState.Loading)

    /**
     * StateFlow emitting the current map state.
     * UI components observe this to render the map, POIs, and handle loading/error states.
     */
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    /**
     * Distance threshold in meters for automatic POI discovery.
     * When the user is within this distance of an undiscovered POI, a discovery request is triggered.
     */
    companion object {
        private const val DISCOVERY_THRESHOLD_METERS = 100.0
    }

    init {
        refreshPois()
    }

    /**
     * Starts observing user location updates for automatic POI discovery.
     * This method is called after location permission is granted.
     */
    private fun observeLocation() {
        scope.launch(cityXploreDispatchers.io) {
            try {
                locationService.observeLocation().collect { location ->
                    checkDiscovery(location)
                }
            } catch (_: Exception) {
                // Location service error - could be permissions or hardware issue
            }
        }
    }

    /**
     * Checks if the user is within discovery range of any undiscovered POIs.
     * If yes, triggers [discoverPoi] for that POI.
     *
     * @param userLocation The current location of the user.
     */
    private fun checkDiscovery(userLocation: Location) {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            currentState.pois.forEach { poi ->
                if (!poi.discovered) {
                    val distance = calculateDistance(
                        userLocation.latitude, userLocation.longitude,
                        poi.latitude, poi.longitude
                    )
                    if (distance < DISCOVERY_THRESHOLD_METERS) {
                        discoverPoi(poi.id)
                    }
                }
            }
        }
    }

    /**
     * Sends a discovery request to the backend for the specified POI.
     * Upon successful discovery, refreshes the POI list to update the UI.
     *
     * @param poiId The unique identifier of the POI to discover.
     */
    private fun discoverPoi(poiId: String) {
        scope.launch(cityXploreDispatchers.io) {
            repository.discoverPoi(poiId).onSuccess {
                refreshPois()
            }
        }
    }

    /**
     * Handles user actions dispatched from the UI.
     *
     * @param action The [MapAction] to process.
     */
    fun onAction(action: MapAction) {
        when (action) {
            MapAction.Refresh -> refreshPois()
            is MapAction.SelectPoi -> selectPoi(action.poiId)
            MapAction.ToggleFollowUser -> toggleFollowState()
            MapAction.PermissionGranted -> observeLocation()
        }
    }

    /**
     * Fetches the latest POI data from the backend and updates the state.
     * Sets state to [MapUiState.Loading] while fetching, then to [MapUiState.Ready]
     * or [MapUiState.Error] based on the result.
     */
    private fun refreshPois() {
        scope.launch(cityXploreDispatchers.io) {
            _state.value = MapUiState.Loading
            val result = repository.fetchPois()
            _state.value = result.fold(
                onSuccess = { pois ->
                    MapUiState.Ready(
                        pois = pois.map(PoiModel::toMapPoi),
                        isFollowingUser = true,
                        selectedPoi = null
                    )
                },
                onFailure = {
                    MapUiState.Error(it.message ?: "Unable to load POIs")
                }
            )
        }
    }

    /**
     * Selects a POI on the map to display its details.
     *
     * @param poiId The unique identifier of the POI to select.
     */
    private fun selectPoi(poiId: String) {
        val current = _state.value
        if (current is MapUiState.Ready) {
            _state.value = current.copy(selectedPoi = current.pois.firstOrNull { it.id == poiId })
        }
    }

    /**
     * Toggles the follow-user-location mode.
     * When enabled, the map camera automatically centers on the user's location.
     */
    private fun toggleFollowState() {
        val current = _state.value
        if (current is MapUiState.Ready) {
            _state.value = current.copy(isFollowingUser = !current.isFollowingUser)
        }
    }
}
