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

/**
 * Options for sorting journal entries.
 */
enum class JournalSort {
    DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC
}

/**
 * Options for filtering journal entries.
 */
enum class JournalFilter {
    ALL, FAVORITES
}

/**
 * ViewModel for the Journal screen.
 *
 * Manages the state of journal entries, including loading, error handling,
 * filtering, sorting, and searching.
 *
 * @property getJournalEntriesUseCase Use case to fetch discovered POIs.
 * @property toggleFavoriteUseCase Use case to toggle favorite status of a POI.
 */
class JournalViewModel(
    private val getJournalEntriesUseCase: GetJournalEntriesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : CityXploreBaseViewModel() {

    private val _rawEntries = MutableStateFlow<List<MapPoi>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    /** Current search query for filtering entries. */
    val searchQuery = MutableStateFlow("")

    /** Current filter applied to entries (e.g., Favorites only). */
    val filter = MutableStateFlow(JournalFilter.ALL)

    /** Current sorting method applied to entries. */
    val sort = MutableStateFlow(JournalSort.DATE_DESC)

    /**
     * UI state exposed to the view.
     * Combines raw entries, loading state, error state, and filter/sort/search params
     * to produce the final filtered and sorted list of entries.
     */
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

    /**
     * Loads journal entries using the [getJournalEntriesUseCase].
     * Updates the loading and error states accordingly.
     */
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

    /**
     * Toggles the favorite status of a POI.
     * Updates the local state optimistically and then calls the use case.
     *
     * @param poi The POI to toggle favorite status for.
     */
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
