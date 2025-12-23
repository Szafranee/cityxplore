package app.cityxplore.map.presentation

import app.cityxplore.core.cityXploreDispatchers
import app.cityxplore.core.location.Location
import app.cityxplore.core.location.LocationService
import app.cityxplore.core.utils.calculateDistance
import app.cityxplore.map.data.PoiRepository
import app.cityxplore.map.domain.PoiModel
import app.cityxplore.map.domain.toMapPoi
import app.cityxplore.platform.CityXploreBaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: PoiRepository,
    private val locationService: LocationService
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    // Distance in meters within which a POI is considered discovered
    companion object {
        private const val DISCOVERY_THRESHOLD_METERS = 100.0
    }

    init {
        refreshPois()
    }

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

    private fun discoverPoi(poiId: String) {
        scope.launch(cityXploreDispatchers.io) {
            repository.discoverPoi(poiId).onSuccess {
                refreshPois()
            }
        }
    }

    fun onAction(action: MapAction) {
        when (action) {
            MapAction.Refresh -> refreshPois()
            is MapAction.SelectPoi -> selectPoi(action.poiId)
            MapAction.ToggleFollowUser -> toggleFollowState()
            MapAction.PermissionGranted -> observeLocation()
        }
    }

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

    private fun selectPoi(poiId: String) {
        val current = _state.value
        if (current is MapUiState.Ready) {
            _state.value = current.copy(selectedPoi = current.pois.firstOrNull { it.id == poiId })
        }
    }

    private fun toggleFollowState() {
        val current = _state.value
        if (current is MapUiState.Ready) {
            _state.value = current.copy(isFollowingUser = !current.isFollowingUser)
        }
    }
}
