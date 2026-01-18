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
import app.cityxplore.social.domain.model.SharedPoi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    private val distanceSyncRepository: DistanceSyncRepository,
    private val sharedPoiRepository: app.cityxplore.social.domain.repository.SharedPoiRepository
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
    private var cachedSharedPois: List<SharedPoi> = emptyList()

    // Track previous level for level-up detection
    private var previousLevel: Int? = null

    init {
        loadData()
        startLocationTracking()
        observeSharedPois()
    }

    /**
     * Observes shared POIs from the repository and updates state.
     */
    private fun observeSharedPois() {
        scope.launch {
            sharedPoiRepository.getReceivedPois().collect { sharedPois ->
                // Filter to only show shared POIs with coordinates (custom POIs)
                val poisWithCoords = sharedPois.filter { it.coordinates != null }
                cachedSharedPois = poisWithCoords

                val currentState = _state.value
                if (currentState is MapUiState.Ready) {
                    _state.value = currentState.copy(sharedPois = poisWithCoords)
                }
            }
        }
        // Initial refresh of shared POIs
        scope.launch {
            sharedPoiRepository.refreshReceivedPois()
        }
    }

    /**
     * Loads/refreshes the user profile and checks for level up.
     *
     * @param checkLevelUp If true, compares with the previous level to detect level up.
     */
    private fun loadProfile(checkLevelUp: Boolean = false) {
        scope.launch {
            val result = profileRepository.getProfile()
            if (result.isSuccess) {
                val newProfile = result.getOrThrow()
                _state.value.let { currentState ->
                    if (currentState is MapUiState.Ready) {
                        val oldLevel = previousLevel
                        val newLevel = newProfile.level

                        // Update the previous level for future comparisons
                        previousLevel = newLevel

                        // Detect level up: the new level is higher than the old level (only if we had a previous level)
                        val leveledUp = checkLevelUp && oldLevel != null && newLevel > oldLevel

                        _state.value = currentState.copy(
                            profile = newProfile,
                            newLevel = if (leveledUp) newLevel else currentState.newLevel
                        )
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
                    warsawHexagons = warsawResult.getOrDefault(emptySet()),
                    sharedPois = cachedSharedPois
                )
                // Trigger a profile load again if loadData finishes later
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
                    loadPois()
                    loadFogOfWar()
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
            MapAction.DismissLevelUpDialog -> dismissLevelUpDialog()
            is MapAction.SelectSharedPoi -> selectSharedPoi(action.sharedPoiId)
            is MapAction.CenterOnLocation -> centerOnLocation(action.latitude, action.longitude)
            is MapAction.DismissSharedDiscoveryNotification -> dismissSharedDiscoveryNotification(action.sharedPoiId)
            is MapAction.ViewDiscoveredSharedPoi -> viewDiscoveredSharedPoi(action.sharedPoiId)
            MapAction.DismissAllSharedDiscoveryNotifications -> dismissAllSharedDiscoveryNotifications()
        }
    }

    /**
     * Centers the map on specific coordinates.
     * Sets the target location that the map component will animate to.
     */
    private fun centerOnLocation(latitude: Double, longitude: Double) {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(
                targetCameraLocation = Location(latitude, longitude),
                isFollowingUser = false // Disable follow mode when manually centering
            )
        }
    }

    /**
     * Handles the "View Details" action for a discovered Shared POI notification.
     * Selects the POI and dismisses its notification.
     */
    private fun viewDiscoveredSharedPoi(sharedPoiId: String) {
        selectSharedPoi(sharedPoiId)
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(
                newlyDiscoveredSharedPoiIds = currentState.newlyDiscoveredSharedPoiIds - sharedPoiId
            )
        }
    }

    /**
     * Dismisses the discovery notification for a specific Shared POI.
     */
    private fun dismissSharedDiscoveryNotification(sharedPoiId: String) {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(
                newlyDiscoveredSharedPoiIds = currentState.newlyDiscoveredSharedPoiIds - sharedPoiId
            )
        }
    }

    /**
     * Dismisses all shared discovery notifications at once.
     */
    private fun dismissAllSharedDiscoveryNotifications() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(newlyDiscoveredSharedPoiIds = emptySet())
        }
    }

    /**
     * Handles the "View Details" action for a discovered POI notification.
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
     * Dismisses the achievement unlock the notification dialogue.
     */
    private fun dismissAchievementNotification() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(newlyUnlockedAchievements = emptyList())
        }
    }

    /**
     * Dismisses the level-up dialogue.
     */
    private fun dismissLevelUpDialog() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(newLevel = null)
        }
    }

    /**
     * Selects a shared POI for viewing details.
     * Also marks it as viewed.
     */
    private fun selectSharedPoi(sharedPoiId: String) {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            val sharedPoi = currentState.sharedPois.find { it.id == sharedPoiId }
            if (sharedPoi != null) {
                _state.value = currentState.copy(
                    selectedSharedPoi = sharedPoi,
                    selectedPoi = null // Clear regular POI selection
                )

                // Mark as viewed if not already
                if (!sharedPoi.isViewed) {
                    scope.launch {
                        sharedPoiRepository.markViewed(sharedPoiId)
                        sharedPoiRepository.refreshReceivedPois()
                    }
                }
            }
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

            // Only show the full loading screen if we don't have data yet
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
            var currentNewLevel: Int? = null
            val currentProfile = if (currentState is MapUiState.Ready) currentState.profile else null

            if (currentState is MapUiState.Ready) {
                currentIsFollowing = currentState.isFollowingUser
                currentRevealedHexagons = currentState.revealedHexagons
                currentWarsawHexagons = currentState.warsawHexagons
                currentUserLocation = currentState.userLocation
                currentAchievements = currentState.newlyUnlockedAchievements
                currentNewlyDiscoveredIds = currentState.newlyDiscoveredPoiIds
                currentNewLevel = currentState.newLevel
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
                    newlyUnlockedAchievements = currentAchievements,
                    newLevel = currentNewLevel
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

                    // Track distance and sync when the threshold reached
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
     * Updates the profile and shows achievement notifications if any were unlocked.
     */
    private fun syncDistance() {
        val distance = distanceTracker.consumeBufferedDistance()
        if (distance <= 0) return

        scope.launch(cityXploreDispatchers.io) {
            distanceSyncRepository.syncDistance(distance)
                .onSuccess { result ->
                    val currentState = _state.value
                    if (currentState is MapUiState.Ready && result.newlyUnlockedAchievements.isNotEmpty()) {
                        // Merge newly unlocked achievements from a distance with any existing ones
                        val mergedAchievements =
                            (currentState.newlyUnlockedAchievements + result.newlyUnlockedAchievements).distinctBy { it.id }

                        _state.value = currentState.copy(
                            newlyUnlockedAchievements = mergedAchievements
                        )
                    }
                    // Refresh profile to get updated total distance and check for level up
                    loadProfile(checkLevelUp = true)
                }
                .onFailure { error ->
                    println("Failed to sync distance: ${error.message}")
                    // Distance is already consumed from buffer - for MVP we accept the loss
                    // In v2: save to local queue for offline sync
                }
        }
    }

    /**
     * Updates the Fog of War based on the user's current location.
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
     * Also, checks for undiscovered shared POIs nearby.
     * Updates the state with newly discovered POI IDs for UI notifications.
     *
     * @param userLocation The current location of the user.
     */
    private fun checkForNearbyPois(userLocation: Location) {
        scope.launch(cityXploreDispatchers.io) {
            // Check regular POIs
            val result = autoDiscoverUseCase.checkAndDiscoverNearbyPois(userLocation)

            result.onSuccess { discoveryResult ->
                if (discoveryResult.newlyDiscoveredPoiIds.isNotEmpty()) {
                    // Refresh POIs to get an updated discovery status
                    val poisResult = getPoisUseCase()

                    poisResult.onSuccess { pois ->
                        val currentState = _state.value
                        if (currentState is MapUiState.Ready) {
                            // Merge newly unlocked achievements from discovery with any existing ones
                            val mergedAchievements =
                                (currentState.newlyUnlockedAchievements + discoveryResult.newlyUnlockedAchievements).distinctBy { it.id }

                            _state.value = currentState.copy(
                                pois = pois.map(PoiModel::toMapPoi),
                                newlyDiscoveredPoiIds = currentState.newlyDiscoveredPoiIds + discoveryResult.newlyDiscoveredPoiIds.toSet(),
                                newlyUnlockedAchievements = mergedAchievements
                            )
                        }

                        // Refresh profile to update XP from achievements and check for level up
                        loadProfile(checkLevelUp = true)
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
            }

            // Check shared POIs for discovery (no XP awarded)
            checkForNearbySharedPois(userLocation)
        }
    }

    /**
     * Checks if any undiscovered shared POIs are within discovery range.
     * Note: Shared POI discoveries do NOT grant XP or count towards regular achievements.
     *
     * @param userLocation The current location of the user.
     */
    private suspend fun checkForNearbySharedPois(userLocation: Location) {
        val currentState = _state.value
        if (currentState !is MapUiState.Ready) return

        val undiscoveredSharedPois = currentState.sharedPois.filter { !it.isDiscovered }
        if (undiscoveredSharedPois.isEmpty()) return

        // Use the same discovery radius as regular POIs (200 m)
        val discoveryRadiusMeters = AutoDiscoverPoisUseCase.DISCOVERY_RADIUS_METERS

        for (sharedPoi in undiscoveredSharedPois) {
            val coords = sharedPoi.coordinates ?: continue
            val distance = calculateHaversineDistance(
                userLocation.latitude, userLocation.longitude,
                coords.first, coords.second
            )

            if (distance <= discoveryRadiusMeters) {
                // Discover the shared POI
                sharedPoiRepository.discoverSharedPoi(sharedPoi.id)
                    .onSuccess { updatedPoi ->
                        // Update the cached shared POIs
                        cachedSharedPois = cachedSharedPois.map {
                            if (it.id == sharedPoi.id) updatedPoi else it
                        }

                        // Update state
                        val freshState = _state.value
                        if (freshState is MapUiState.Ready) {
                            _state.value = freshState.copy(
                                sharedPois = cachedSharedPois,
                                newlyDiscoveredSharedPoiIds = freshState.newlyDiscoveredSharedPoiIds + sharedPoi.id
                            )
                        }
                    }
                    .onFailure { error ->
                        println("Failed to discover shared POI ${sharedPoi.id}: ${error.message}")
                    }
            }
        }
    }

    /**
     * Calculates distance between two coordinates using Haversine formula.
     */
    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // meters

        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
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
                selectedPoi = currentState.pois.firstOrNull { it.id == poiId },
                selectedSharedPoi = null // Clear shared POI selection
            )
        }
    }

    private fun deselectPoi() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(
                selectedPoi = null,
                selectedSharedPoi = null
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
