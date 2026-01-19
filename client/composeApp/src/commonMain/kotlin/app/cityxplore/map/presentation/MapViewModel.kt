package app.cityxplore.map.presentation

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.core.cache.CacheKey
import app.cityxplore.core.cache.CacheManager
import app.cityxplore.core.cache.CacheState
import app.cityxplore.core.cityXploreDispatchers
import app.cityxplore.core.lifecycle.AppLifecycleObserver
import app.cityxplore.core.lifecycle.AppLifecycleState
import app.cityxplore.core.location.DistanceTracker
import app.cityxplore.core.location.Location
import app.cityxplore.core.location.LocationService
import app.cityxplore.core.utils.calculateDistance
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
import app.cityxplore.social.domain.repository.SharedPoiRepository
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
 * - Managing Fog of War (revealed hexagons) with offline-first approach
 * - Tracking distance travelled and syncing to backend
 * - Displaying achievement unlock notifications
 * - Lifecycle-aware caching to prevent unnecessary reloads
 *
 * POIs are automatically discovered when the user is within the discovery radius
 * (defined in [AutoDiscoverPoisUseCase]). The discovery is handled by the use case,
 * and the ViewModel refreshes the POI list upon successful discoveries.
 *
 * @property getPoisUseCase Use case for fetching POIs with discovery status.
 * @property autoDiscoverUseCase Use case for automatic POI discovery based on location.
 * @property updateFogOfWarUseCase Use case for updating fog of war based on user location.
 * @property fogOfWarRepository Repository for fetching revealed hexagons (offline-first).
 * @property locationService The service providing user location updates.
 * @property distanceTracker Tracker for accumulating distance between GPS points.
 * @property distanceSyncRepository Repository for syncing distance to the backend.
 * @property cacheManager Manager for tracking data freshness.
 * @property appLifecycleObserver Observer for app lifecycle events.
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
    private val sharedPoiRepository: SharedPoiRepository,
    private val cacheManager: CacheManager,
    private val appLifecycleObserver: AppLifecycleObserver
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<MapUiState>(MapUiState.Loading)

    /**
     * StateFlow emitting the current map state.
     * UI components observe this to render the map, POIs, and handle loading/error states.
     */
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private var locationObserverJob: Job? = null
    private var fogOfWarObserverJob: Job? = null
    private var lastKnownLocation: Location? = null
    private var cachedWarsawHexagons: Set<String> = emptySet()
    private var cachedSharedPois: List<SharedPoi> = emptyList()

    // Track previous level for level-up detection
    private var previousLevel: Int? = null

    init {
        observeLifecycle()
        observeFogOfWar()
        loadDataIfNeeded()
        startLocationTracking()
        observeSharedPois()
    }

    /**
     * Observes app lifecycle to handle resume events.
     * Prevents unnecessary data reloads on quick app switches.
     */
    private fun observeLifecycle() {
        scope.launch {
            appLifecycleObserver.lifecycleState.collect { state ->
                when (state) {
                    AppLifecycleState.RESUMED -> handleResume()
                    else -> { /* no action needed */
                    }
                }
            }
        }
    }

    /**
     * Handles app resume - decides whether to refresh data based on background duration.
     */
    private fun handleResume() {
        if (appLifecycleObserver.wasQuickSwitch()) {
            // Quick switch - don't reload anything
            return
        }

        if (appLifecycleObserver.shouldRefreshOnResume()) {
            // Long background - refresh data in the background
            refreshDataInBackground()
        }
    }

    /**
     * Refreshes data in the background without showing loading state.
     */
    private fun refreshDataInBackground() {
        scope.launch(cityXploreDispatchers.io) {
            // Refresh fog of war from server (will update local DB, Flow will notify)
            fogOfWarRepository.refreshRevealedHexagons()
            cacheManager.markAsFresh(CacheKey.FOG_OF_WAR)

            // Refresh POIs
            loadPois()
            cacheManager.markAsFresh(CacheKey.POIS)
        }
    }

    /**
     * Observes revealed hexagons from the local database.
     * This is the primary source of truth for fog of war - updates automatically.
     */
    private fun observeFogOfWar() {
        fogOfWarObserverJob?.cancel()
        fogOfWarObserverJob = scope.launch(cityXploreDispatchers.io) {
            fogOfWarRepository.observeRevealedHexagons().collect { revealedHexagons ->
                val currentState = _state.value
                if (currentState is MapUiState.Ready) {
                    _state.value = currentState.copy(revealedHexagons = revealedHexagons)
                }
            }
        }
    }

    /**
     * Loads data based on cache freshness.
     * - FRESH: Use cached data, don't load
     * - STALE: Use cached data, refresh in background
     * - EXPIRED/EMPTY: Full load
     */
    private fun loadDataIfNeeded() {
        val fogOfWarCacheState = cacheManager.getCacheState(CacheKey.FOG_OF_WAR)
        val poisCacheState = cacheManager.getCacheState(CacheKey.POIS)

        when {
            fogOfWarCacheState == CacheState.FRESH && poisCacheState == CacheState.FRESH -> {
                // Everything is fresh - just load from local
                loadDataFromLocal()
            }

            fogOfWarCacheState == CacheState.EMPTY || poisCacheState == CacheState.EMPTY -> {
                // First load - full load with loading state
                loadData()
            }

            else -> {
                // Stale or expired - load from local, refresh in background
                loadDataFromLocal()
                refreshDataInBackground()
            }
        }
    }

    /**
     * Loads data from local sources only (fast, no network).
     */
    private fun loadDataFromLocal() {
        scope.launch(cityXploreDispatchers.io) {
            val revealedResult = fogOfWarRepository.getRevealedHexagons()
            val warsawResult = fogOfWarRepository.getWarsawHexagons()

            cachedWarsawHexagons = warsawResult.getOrElse { cachedWarsawHexagons }

            // Set initial state - revealed hexagons will be updated by Flow
            _state.value = MapUiState.Ready(
                pois = emptyList(), // Will be loaded
                userLocation = lastKnownLocation,
                isFollowingUser = true,
                selectedPoi = null,
                newlyDiscoveredPoiIds = emptySet(),
                revealedHexagons = revealedResult.getOrDefault(emptySet()),
                warsawHexagons = cachedWarsawHexagons,
                sharedPois = cachedSharedPois
            )

            // Load POIs (will update state)
            loadPois()
            loadProfile()
        }
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
            MapAction.ClearTargetCameraLocation -> clearTargetCameraLocation()
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
     * Clears the target camera location after animation completes.
     * This allows the same location to be targeted again.
     */
    private fun clearTargetCameraLocation() {
        val currentState = _state.value
        if (currentState is MapUiState.Ready) {
            _state.value = currentState.copy(targetCameraLocation = null)
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
            var currentRevealedHexagons = emptySet<String>()
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
     * Refreshes revealed hexagons from server - local DB will be updated,
     * and the observing Flow will automatically update the UI.
     */
    private fun loadFogOfWar() {
        scope.launch(cityXploreDispatchers.io) {
            // Refresh from server - this updates local DB, and Flow will notify UI
            fogOfWarRepository.refreshRevealedHexagons()

            val warsawResult = fogOfWarRepository.getWarsawHexagons()
            cachedWarsawHexagons = warsawResult.getOrElse { cachedWarsawHexagons }

            val currentState = _state.value
            if (currentState is MapUiState.Ready) {
                _state.value = currentState.copy(
                    warsawHexagons = cachedWarsawHexagons
                )
            }

            cacheManager.markAsFresh(CacheKey.FOG_OF_WAR)
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
     * Checks if any undiscovered shared POIs are within the discovery range.
     * Note: Shared POI discoveries do NOT grant XP or count towards regular achievements.
     *
     * @param userLocation The current location of the user.
     */
    private suspend fun checkForNearbySharedPois(userLocation: Location) {
        val currentState = _state.value
        if (currentState !is MapUiState.Ready) return

        // Use the same discovery radius as regular POIs (200 m)
        val discoveryRadiusMeters = AutoDiscoverPoisUseCase.DISCOVERY_RADIUS_METERS

        // 1. Identify undiscovered shared POIs within range
        val poisToDiscover = currentState.sharedPois.filter { sharedPoi ->
            if (sharedPoi.isDiscovered) return@filter false

            val coords = sharedPoi.coordinates ?: return@filter false
            val distance = calculateDistance(
                userLocation.latitude, userLocation.longitude,
                coords.first, coords.second
            )
            distance <= discoveryRadiusMeters
        }

        if (poisToDiscover.isEmpty()) return

        // 2. Discover them sequentially and collect results to avoid race conditions
        val successfullyDiscovered = mutableListOf<SharedPoi>()

        poisToDiscover.forEach { poi ->
            sharedPoiRepository.discoverSharedPoi(poi.id)
                .onSuccess { updatedPoi ->
                    successfullyDiscovered.add(updatedPoi)
                }
                .onFailure { error ->
                    println("Failed to discover shared POI ${poi.id}: ${error.message}")
                }
        }

        if (successfullyDiscovered.isEmpty()) return

        // 3. Batch update state once
        val freshState = _state.value
        if (freshState is MapUiState.Ready) {
            // Update the cached list first
            cachedSharedPois = cachedSharedPois.map { current ->
                successfullyDiscovered.find { it.id == current.id } ?: current
            }

            // Update UI state
            _state.value = freshState.copy(
                sharedPois = cachedSharedPois,
                newlyDiscoveredSharedPoiIds = freshState.newlyDiscoveredSharedPoiIds + successfullyDiscovered.map { it.id }
                    .toSet()
            )
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
        fogOfWarObserverJob?.cancel()
    }
}
