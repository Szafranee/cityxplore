package app.cityxplore.map.presentation

import app.cityxplore.map.domain.MapPoi

sealed interface MapUiState {
    data object Loading : MapUiState
    data class Ready(
        val pois: List<MapPoi>,
        val isFollowingUser: Boolean,
        val selectedPoi: MapPoi?
    ) : MapUiState

    data class Error(val message: String) : MapUiState
}

sealed interface MapAction {
    data object Refresh : MapAction
    data object ToggleFollowUser : MapAction
    data class SelectPoi(val poiId: String) : MapAction
}
