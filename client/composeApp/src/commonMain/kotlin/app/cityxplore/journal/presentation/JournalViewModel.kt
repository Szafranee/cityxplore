package app.cityxplore.journal.presentation

import app.cityxplore.core.cityXploreDispatchers
import app.cityxplore.journal.domain.GetJournalEntriesUseCase
import app.cityxplore.journal.domain.ToggleFavoriteUseCase
import app.cityxplore.map.domain.MapPoi
import app.cityxplore.platform.CityXploreBaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class JournalSort {
    DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC
}

enum class JournalFilter {
    ALL, FAVORITES
}

class JournalViewModel(
    private val getJournalEntriesUseCase: GetJournalEntriesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : CityXploreBaseViewModel() {

    private val _rawEntries = MutableStateFlow<List<MapPoi>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val searchQuery = MutableStateFlow("")
    val filter = MutableStateFlow(JournalFilter.ALL)
    val sort = MutableStateFlow(JournalSort.DATE_DESC)

    val state: StateFlow<JournalUiState> = combine(
        _rawEntries,
        _isLoading,
        _error,
        combine(searchQuery, filter, sort) { query, filter, sort -> Triple(query, filter, sort) }
    ) { entries, isLoading, error, params ->
        val (query, currentFilter, currentSort) = params
        if (isLoading) {
            JournalUiState.Loading
        } else if (error != null) {
            JournalUiState.Error(error)
        } else {
            val filtered = entries.filter { poi ->
                val matchesQuery = poi.name.contains(query, ignoreCase = true)
                val matchesFilter = when (currentFilter) {
                    JournalFilter.ALL -> true
                    JournalFilter.FAVORITES -> poi.isFavorite
                }
                matchesQuery && matchesFilter
            }.sortedWith { a, b ->
                when (currentSort) {
                    JournalSort.DATE_DESC -> (b.discoveryDate ?: 0L).compareTo(a.discoveryDate ?: 0L)
                    JournalSort.DATE_ASC -> (a.discoveryDate ?: 0L).compareTo(b.discoveryDate ?: 0L)
                    JournalSort.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                    JournalSort.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                }
            }
            JournalUiState.Content(filtered)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), JournalUiState.Loading)

    init {
        loadEntries()
    }

    fun loadEntries() {
        scope.launch(cityXploreDispatchers.io) {
            _isLoading.value = true
            _error.value = null
            getJournalEntriesUseCase()
                .onSuccess { entries ->
                    _rawEntries.value = entries
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Unknown error"
                    _isLoading.value = false
                }
        }
    }

    fun toggleFavorite(poi: MapPoi) {
        scope.launch(cityXploreDispatchers.io) {
            // Optimistic update
            val currentEntries = _rawEntries.value
            val updatedEntries = currentEntries.map {
                if (it.id == poi.id) it.copy(isFavorite = !it.isFavorite) else it
            }
            _rawEntries.value = updatedEntries

            toggleFavoriteUseCase(poi.id).onFailure {
                // Revert on failure
                _rawEntries.value = currentEntries
            }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilter(newFilter: JournalFilter) {
        filter.value = newFilter
    }

    fun setSort(newSort: JournalSort) {
        sort.value = newSort
    }
}
