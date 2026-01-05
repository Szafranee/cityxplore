package app.cityxplore.map.presentation

import app.cityxplore.core.cityXploreDispatchers
import app.cityxplore.core.location.Location
import app.cityxplore.core.location.LocationService
import app.cityxplore.map.domain.AutoDiscoverPoisUseCase
import app.cityxplore.map.domain.FogOfWarRepository
import app.cityxplore.map.domain.GetPoisWithDiscoveriesUseCase
import app.cityxplore.map.domain.PoiModel
import app.cityxplore.map.domain.UpdateFogOfWarUseCase
import app.cityxplore.map.domain.toMapPoi
import app.cityxplore.platform.CityXploreBaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the map screen state and POI discovery logic.
 *
 * This ViewModel handles:
 * - Fetching POI data with discovery status from the backend
 * - Observing user location to trigger automatic POI discovery
 * - Managing map camera follow mode
 * - Handling POI selection for detail display
 * - Tracking newly discovered POIs for UI notifications
 * - Managing Fog of War (revealed hexagons)
 *
 * POIs are automatically discovered when the user is within the discovery radius
 * (defined in [AutoDiscoverPoisUseCase]). The discovery is handled by the use case,
 * and the ViewModel refreshes the POI list upon successful discoveries.
 *
 * @property getPoisUseCase Use case for fetching POIs with discovery status.
 * @property autoDiscoverUseCase Use case for automatic POI discovery based on location.
 * @property updateFogOfWarUseCase Use case for updating fog of war based on user location.
 * @property fogOfWarRepository Repository for fetching revealed hexagons.
 * @property locationService The service providing user location updates.
 */
class MapViewModel(
    private val getPoisUseCase: GetPoisWithDiscoveriesUseCase,
    private val autoDiscoverUseCase: AutoDiscoverPoisUseCase,
    private val updateFogOfWarUseCase: UpdateFogOfWarUseCase,
    private val fogOfWarRepository: FogOfWarRepository,
    private val locationService: LocationService
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<MapUiState>(MapUiState.Loading)

    /**
     * StateFlow emitting the current map state.
     * UI components observe this to render the map, POIs, and handle loading/error states.
     */
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private var locationObserverJob: Job? = null
    private var lastKnownLocation: Location? = null
    private var cachedWarsawHexagons: Set<String> = emptySet()
    private var cachedRevealedHexagons: Set<String> = emptySet()

    init {
        loadPois()
        loadFogOfWar()
    }

    /**
     * Handles user actions dispatched from the UI.
     *
     * @param action The [MapAction] to process.
     */
    fun onAction(action: MapAction) {
        when (action) {
            MapAction.Refresh -> {
                loadPois()
                loadFogOfWar()
            }

            is MapAction.SelectPoi -> selectPoi(action.poiId)
            MapAction.ToggleFollowUser -> toggleFollowState()
            MapAction.PermissionGranted -> startLocationTracking()
            is MapAction.UpdateLocation -> updateUserLocation(action.location)
            is MapAction.DismissDiscoveryNotification -> dismissDiscoveryNotification(action.poiId)
        }
    }

    /**
     * Fetches POIs with discovery status from the backend and updates the state.
     * Sets state to [MapUiState.Loading] while fetching, then to [MapUiState.Ready]
     * or [MapUiState.Error] based on the result.
     */
    private fun loadPois() {
        scope.launch(cityXploreDispatchers.io) {
            _state.value = MapUiState.Loading

            // Preserve the current follow flag from existing state
            val previousIsFollowing = when (val s = _state.value) {
                is MapUiState.Ready -> s.isFollowingUser
                else -> true
            }

            val result = getPoisUseCase()
            _state.value = result.fold(
                onSuccess = { pois ->
                    MapUiState.Ready(
                        pois = pois.map(PoiModel::toMapPoi),
                        userLocation = lastKnownLocation,
                        isFollowingUser = previousIsFollowing,
                        selectedPoi = null,
                        newlyDiscoveredPoiIds = emptySet(),
                        revealedHexagons = cachedRevealedHexagons,
                        warsawHexagons = cachedWarsawHexagons
                    )
                },
                onFailure = { error ->
                    MapUiState.Error(error.message ?: "Unable to load POIs")
                }
            )
        }
    }

    /**
     * Loads the Fog of War state from the backend.
     * Fetches revealed hexagons and updates the map state.
     */
    private fun loadFogOfWar() {
        scope.launch(cityXploreDispatchers.io) {
            val revealedResult = fogOfWarRepository.getRevealedHexagons()
            val warsawResult = fogOfWarRepository.getWarsawHexagons()

            cachedRevealedHexagons = revealedResult.getOrElse { cachedRevealedHexagons }
            cachedWarsawHexagons = warsawResult.getOrElse { cachedWarsawHexagons }

            val currentState = _state.value
            if (currentState is MapUiState.Ready) {
                _state.value = currentState.copy(
                    revealedHexagons = cachedRevealedHexagons,
                    warsawHexagons = cachedWarsawHexagons
                )
            }
        }
    }

    /**
     * Starts observing user location updates for automatic POI discovery.
     * This method is called after location permission is granted.
     */
    private fun startLocationTracking() {
        locationObserverJob?.cancel()
        locationObserverJob = scope.launch(cityXploreDispatchers.io) {
            try {
                locationService.observeLocation().collect { location ->
                    lastKnownLocation = location
                    updateUserLocation(location)
                    checkForNearbyPois(location)
                    updateFogOfWar(location)
                }
            } catch (_: Exception) {
                // Location service error - could be permissions or hardware issue
                // Keep current state, don't crash the app
            }
        }
    }

    /**
     * Updates the Fog of War based on user's current location.
     * Reveals hexagons within the configured radius.
     *
     * @param location The user's current location.
     */
    private fun updateFogOfWar(location: Location) {
        scope.launch(cityXploreDispatchers.io) {
            val result = updateFogOfWarUseCase(location)
            result.onSuccess { newHexCount ->
                if (newHexCount > 0) {
                    // Reload revealed hexagons from repository
                    loadFogOfWar()
                }
            }
            // Silently ignore errors
        }
    }

    /**
     * Updates the user's location in the current state.
     *
     * @param location The new user location.
     */
    private fun updateUserLocation(location: Location) {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(userLocation = location)
        }
    }

    /**
     * Checks if any undiscovered POIs are within discovery range and triggers discovery.
     * Updates the state with newly discovered POI IDs for UI notifications.
     *
     * @param userLocation The current location of the user.
     */
    private fun checkForNearbyPois(userLocation: Location) {
        scope.launch(cityXploreDispatchers.io) {
            val result = autoDiscoverUseCase.checkAndDiscoverNearbyPois(userLocation)

            result.onSuccess { newlyDiscoveredIds ->
                if (newlyDiscoveredIds.isNotEmpty()) {
                    // Refresh POIs to get updated discovery status
                    val poisResult = getPoisUseCase()

                    poisResult.onSuccess { pois ->
                        val currentState = _state.value
                        if (currentState is MapUiState.Ready) {
                            _state.value = currentState.copy(
                                pois = pois.map(PoiModel::toMapPoi),
                                newlyDiscoveredPoiIds = newlyDiscoveredIds.toSet()
                            )
                        }
                    }

                    poisResult.onFailure { error ->
                        // Log error and clear discovery state to prevent inconsistency
                        println("Failed to refresh POIs after discovery: ${error.message}")
                        val currentState = _state.value
                        if (currentState is MapUiState.Ready) {
                            // Keep current state but clear newly discovered IDs
                            _state.value = currentState.copy(
                                newlyDiscoveredPoiIds = emptySet()
                            )
                        }
                    }
                }
            }

            result.onFailure { error ->
                // Log auto-discovery failure
                println("Auto-discovery failed: ${error.message}")
                // Optionally emit a UI error event here if needed
            }
        }
    }

    /**
     * Selects a POI on the map to display its details.
     *
     * @param poiId The unique identifier of the POI to select.
     */
    private fun selectPoi(poiId: String) {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(
                selectedPoi = currentState.pois.firstOrNull { it.id == poiId }
            )
        }
    }

    /**
     * Toggles the follow-user-location mode.
     * When enabled, the map camera automatically centers on the user's location.
     */
    private fun toggleFollowState() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(
                isFollowingUser = !currentState.isFollowingUser
            )
        }
    }

    /**
     * Dismisses the discovery notification for a specific POI.
     *
     * @param poiId The ID of the POI whose notification should be dismissed.
     */
    private fun dismissDiscoveryNotification(poiId: String) {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(
                newlyDiscoveredPoiIds = currentState.newlyDiscoveredPoiIds - poiId
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationObserverJob?.cancel()
    }
}
