package app.cityxplore.journal.presentation

import app.cityxplore.map.domain.MapPoi

sealed interface JournalUiState {
    data object Loading : JournalUiState
    data class Content(val entries: List<MapPoi>) : JournalUiState
    data class Error(val message: String) : JournalUiState
}
