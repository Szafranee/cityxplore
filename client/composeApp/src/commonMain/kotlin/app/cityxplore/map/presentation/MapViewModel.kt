package app.cityxplore.map.presentation

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.core.cityXploreDispatchers
import app.cityxplore.core.location.DistanceTracker
import app.cityxplore.core.location.Location
import app.cityxplore.core.location.LocationService
import app.cityxplore.journal.domain.ToggleFavoriteUseCase
import app.cityxplore.map.domain.AutoDiscoverPoisUseCase
import app.cityxplore.map.domain.FogOfWarRepository
import app.cityxplore.map.domain.GetPoisWithDiscoveriesUseCase
import app.cityxplore.map.domain.PoiModel
import app.cityxplore.map.domain.UpdateFogOfWarUseCase
import app.cityxplore.map.domain.toMapPoi
import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.profile.domain.DistanceSyncRepository
import app.cityxplore.profile.domain.ProfileRepository
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
 * - Tracking distance travelled and syncing to backend
 * - Displaying achievement unlock notifications
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
 * @property distanceTracker Tracker for accumulating distance between GPS points.
 * @property distanceSyncRepository Repository for syncing distance to the backend.
 */
class MapViewModel(
    private val getPoisUseCase: GetPoisWithDiscoveriesUseCase,
    private val autoDiscoverUseCase: AutoDiscoverPoisUseCase,
    private val updateFogOfWarUseCase: UpdateFogOfWarUseCase,
    private val fogOfWarRepository: FogOfWarRepository,
    private val locationService: LocationService,
    private val profileRepository: ProfileRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val distanceTracker: DistanceTracker,
    private val distanceSyncRepository: DistanceSyncRepository
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
        loadData()
        startLocationTracking()
    }

    private fun loadProfile() {
        scope.launch {
            val result = profileRepository.getProfile()
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                _state.value.let { currentState ->
                    if (currentState is MapUiState.Ready) {
                        _state.value = currentState.copy(profile = profile)
                    }
                }
            }
        }
    }

    /**
     * Loads POI data and initialised map state.
     * Fetches POIs and revealed hexagons, then updates the state to [MapUiState.Ready]
     * or [MapUiState.Error] based on the result.
     */
    private fun loadData() {
        scope.launch(cityXploreDispatchers.io) {
            // Initial load
            val poisResult = getPoisUseCase()
            val revealedResult = fogOfWarRepository.getRevealedHexagons()
            val warsawResult = fogOfWarRepository.getWarsawHexagons()

            cachedRevealedHexagons = revealedResult.getOrElse { cachedRevealedHexagons }
            cachedWarsawHexagons = warsawResult.getOrElse { cachedWarsawHexagons }

            poisResult.onSuccess { pois ->
                _state.value = MapUiState.Ready(
                    pois = pois.map { it.toMapPoi() },
                    userLocation = null,
                    isFollowingUser = true,
                    selectedPoi = null,
                    newlyDiscoveredPoiIds = emptySet(),
                    revealedHexagons = revealedResult.getOrDefault(emptySet()),
                    warsawHexagons = warsawResult.getOrDefault(emptySet())
                )
                // Trigger profile load again if loadData finishes later
                loadProfile()
            }

            poisResult.onFailure { error ->
                _state.value = MapUiState.Error(error.message ?: "Unable to load POIs")
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
            MapAction.Refresh -> {
                scope.launch(cityXploreDispatchers.io) {
                    loadData()
                }
            }

            MapAction.RefreshPois -> {
                scope.launch(cityXploreDispatchers.io) {
                    loadPois()
                }
            }

            is MapAction.SelectPoi -> selectPoi(action.poiId)
            MapAction.DeselectPoi -> deselectPoi()
            MapAction.ToggleFollowUser -> toggleFollowState()
            MapAction.PermissionGranted -> startLocationTracking()
            is MapAction.UpdateLocation -> updateUserLocation(action.location)
            is MapAction.DismissDiscoveryNotification -> dismissDiscoveryNotification(action.poiId)
            is MapAction.ToggleFavorite -> toggleFavorite(action.poiId)
            is MapAction.ViewDiscoveredPoi -> viewDiscoveredPoi(action.poiId)
            MapAction.DismissAllDiscoveryNotifications -> dismissAllDiscoveryNotifications()
            MapAction.DismissAchievementNotification -> dismissAchievementNotification()
        }
    }

    /**
     * Handles "View Details" action for a discovered POI notification.
     * Selects the POI and dismisses its notification.
     */
    private fun viewDiscoveredPoi(poiId: String) {
        selectPoi(poiId)
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(
                newlyDiscoveredPoiIds = currentState.newlyDiscoveredPoiIds - poiId
            )
        }
    }

    /**
     * Dismisses all discovery notifications at once.
     */
    private fun dismissAllDiscoveryNotifications() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(newlyDiscoveredPoiIds = emptySet())
        }
    }

    /**
     * Dismisses the achievement unlock notification dialog.
     */
    private fun dismissAchievementNotification() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(newlyUnlockedAchievements = emptyList())
        }
    }

    private fun toggleFavorite(poiId: String) {
        scope.launch(cityXploreDispatchers.io) {
            val currentState = _state.value
            if (currentState is MapUiState.Ready) {
                // Optimistic update
                val updatedPois = currentState.pois.map {
                    if (it.id == poiId) it.copy(isFavorite = !it.isFavorite) else it
                }
                val updatedSelected = if (currentState.selectedPoi?.id == poiId) {
                    currentState.selectedPoi.copy(isFavorite = !currentState.selectedPoi.isFavorite)
                } else currentState.selectedPoi

                _state.value = currentState.copy(
                    pois = updatedPois,
                    selectedPoi = updatedSelected
                )

                toggleFavoriteUseCase(poiId).onFailure {
                    // Revert only specific changes on the fresh state
                    val freshState = _state.value
                    if (freshState is MapUiState.Ready) {
                        val revertedPois = freshState.pois.map {
                            if (it.id == poiId) it.copy(isFavorite = !it.isFavorite) else it
                        }
                        val revertedSelected = if (freshState.selectedPoi?.id == poiId) {
                            freshState.selectedPoi.copy(isFavorite = !freshState.selectedPoi.isFavorite)
                        } else freshState.selectedPoi

                        _state.value = freshState.copy(
                            pois = revertedPois,
                            selectedPoi = revertedSelected
                        )
                    }
                }
            }
        }
    }

    /**
     * Fetches POIs with discovery status from the backend and updates the state.
     * Sets state to [MapUiState.Loading] only if not already [MapUiState.Ready].
     * Then updates to new [MapUiState.Ready] or [MapUiState.Error] based on the result.
     */
    private fun loadPois() {
        scope.launch(cityXploreDispatchers.io) {
            val currentState = _state.value

            // Only show full loading screen if we don't have data yet
            if (currentState !is MapUiState.Ready) {
                _state.value = MapUiState.Loading
            }

            // Default values to fall back on if we are not in Ready state
            var currentIsFollowing = true
            var currentRevealedHexagons = cachedRevealedHexagons
            var currentWarsawHexagons = cachedWarsawHexagons
            var currentUserLocation: Location? = lastKnownLocation
            var currentAchievements: List<Achievement> = emptyList()
            var currentNewlyDiscoveredIds: Set<String> = emptySet()
            val currentProfile = if (currentState is MapUiState.Ready) currentState.profile else null

            if (currentState is MapUiState.Ready) {
                currentIsFollowing = currentState.isFollowingUser
                currentRevealedHexagons = currentState.revealedHexagons
                currentWarsawHexagons = currentState.warsawHexagons
                currentUserLocation = currentState.userLocation
                currentAchievements = currentState.newlyUnlockedAchievements
                currentNewlyDiscoveredIds = currentState.newlyDiscoveredPoiIds
            }

            val result = getPoisUseCase()

            result.onSuccess { pois ->
                _state.value = MapUiState.Ready(
                    pois = pois.map(PoiModel::toMapPoi),
                    userLocation = currentUserLocation,
                    isFollowingUser = currentIsFollowing,
                    selectedPoi = null,
                    newlyDiscoveredPoiIds = currentNewlyDiscoveredIds,
                    revealedHexagons = currentRevealedHexagons,
                    warsawHexagons = currentWarsawHexagons,
                    profile = currentProfile,
                    newlyUnlockedAchievements = currentAchievements
                )
            }

            result.onFailure { error ->
                if (currentState !is MapUiState.Ready) {
                    _state.value = MapUiState.Error(error.message ?: "Unable to load POIs")
                } else {
                    // If we were ready, we just stay ready with old data (and maybe log error)
                    println("Failed to refresh POIs: ${error.message}")
                }
            }
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
     * Starts observing user location updates for automatic POI discovery and distance tracking.
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

                    // Track distance and sync when threshold reached
                    val shouldSync = distanceTracker.onNewLocation(location)
                    if (shouldSync) {
                        syncDistance()
                    }
                }
            } catch (_: Exception) {
                // Location service error - could be permissions or hardware issue
                // Keep current state, don't crash the app
            }
        }
    }

    /**
     * Syncs accumulated distance to the backend.
     * Updates profile and shows achievement notifications if any were unlocked.
     */
    private fun syncDistance() {
        val distance = distanceTracker.consumeBufferedDistance()
        if (distance <= 0) return

        scope.launch(cityXploreDispatchers.io) {
            distanceSyncRepository.syncDistance(distance)
                .onSuccess { result ->
                    val currentState = _state.value
                    if (currentState is MapUiState.Ready && result.newlyUnlockedAchievements.isNotEmpty()) {
                        _state.value = currentState.copy(
                            newlyUnlockedAchievements = result.newlyUnlockedAchievements
                        )
                    }
                    // Refresh profile to get updated total distance
                    loadProfile()
                }
                .onFailure { error ->
                    println("Failed to sync distance: ${error.message}")
                    // Distance is already consumed from buffer - for MVP we accept the loss
                    // In v2: save to local queue for offline sync
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
                    // Reload revealed hexagons from the repository
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

            result.onSuccess { discoveryResult ->
                if (discoveryResult.newlyDiscoveredPoiIds.isNotEmpty()) {
                    // Refresh POIs to get updated discovery status
                    val poisResult = getPoisUseCase()

                    poisResult.onSuccess { pois ->
                        val currentState = _state.value
                        if (currentState is MapUiState.Ready) {
                            // Merge newly unlocked achievements from discovery with any existing ones
                            val mergedAchievements =
                                (currentState.newlyUnlockedAchievements + discoveryResult.newlyUnlockedAchievements).distinctBy { it.id }

                            _state.value = currentState.copy(
                                pois = pois.map(PoiModel::toMapPoi),
                                newlyDiscoveredPoiIds = discoveryResult.newlyDiscoveredPoiIds.toSet(),
                                newlyUnlockedAchievements = mergedAchievements
                            )
                        }
                    }

                    poisResult.onFailure { error ->
                        // Log error and clear discovery state to prevent inconsistency
                        println("Failed to refresh POIs after discovery: ${error.message}")
                        val currentState = _state.value
                        if (currentState is MapUiState.Ready) {
                            // Keep the current state but clear newly discovered IDs
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

    private fun deselectPoi() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(selectedPoi = null)
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
