package app.cityxplore.journal.presentation

import app.cityxplore.core.cache.CacheKey
import app.cityxplore.core.cache.CacheManager
import app.cityxplore.core.cache.CacheState
import app.cityxplore.core.cityXploreDispatchers
import app.cityxplore.core.lifecycle.AppLifecycleObserver
import app.cityxplore.core.lifecycle.AppLifecycleState
import app.cityxplore.journal.domain.ToggleFavoriteUseCase
import app.cityxplore.map.data.PoiRepository
import app.cityxplore.map.domain.MapPoi
import app.cityxplore.map.domain.toMapPoi
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
 * Offline-first, lifecycle-aware ViewModel for the Journal screen.
 *
 * Key behaviors:
 * - **Reading:** Observes Flow from local Room database (single source of truth)
 * - **Refreshing:** Triggers network refresh, Room updates automatically
 * - **Lifecycle:** Doesn't reload on quick app switches, background refresh on long pause
 *
 * @property poiRepository Repository for POI data (offline-first).
 * @property toggleFavoriteUseCase Use case to toggle favorite status of a POI.
 * @property cacheManager Manager for tracking data freshness.
 * @property appLifecycleObserver Observer for app lifecycle events.
 */
class JournalViewModel(
    private val poiRepository: PoiRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val cacheManager: CacheManager,
    private val appLifecycleObserver: AppLifecycleObserver
) : CityXploreBaseViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    /** Current search query for filtering entries. */
    val searchQuery = MutableStateFlow("")

    /** Current filter applied to entries (e.g. Favorites only). */
    val filter = MutableStateFlow(JournalFilter.ALL)

    /** Current sorting method applied to entries. */
    val sort = MutableStateFlow(JournalSort.DATE_DESC)

    /**
     * UI state exposed to the view.
     * Observes discovered POIs from Room and combines with filter/sort/search params.
     */
    val state: StateFlow<JournalUiState> = combine(
        poiRepository.observeDiscoveredPois(),
        _isLoading,
        _error,
        combine(searchQuery, filter, sort) { query, filter, sort -> Triple(query, filter, sort) }
    ) { pois, isLoading, error, params ->
        val (query, currentFilter, currentSort) = params

        // Map domain models to MapPoi for UI
        val entries = pois.map { it.toMapPoi() }

        if (isLoading && entries.isEmpty()) {
            JournalUiState.Loading
        } else if (error != null && entries.isEmpty()) {
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
        observeLifecycle()
        loadDataIfNeeded()
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
            refreshInBackground()
        }
    }

    /**
     * Loads data based on the cache state.
     */
    private fun loadDataIfNeeded() {
        when (cacheManager.getCacheState(CacheKey.POIS)) {
            CacheState.FRESH -> { /* Already have fresh data, Room Flow will provide it */
            }

            CacheState.STALE -> refreshInBackground()
            CacheState.EXPIRED, CacheState.EMPTY -> loadEntries()
        }
    }

    /**
     * Triggers a full refresh with a loading indicator.
     * Called on an initial load or manual pull-to-refresh.
     */
    fun loadEntries() {
        scope.launch(cityXploreDispatchers.io) {
            _isLoading.value = true
            _error.value = null

            poiRepository.refreshPois()
                .onSuccess {
                    cacheManager.markAsFresh(CacheKey.POIS)
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Unknown error"
                    _isLoading.value = false
                }
        }
    }

    /**
     * Refreshes data in the background without showing loading state.
     * Used when returning from the background after a long pause.
     */
    private fun refreshInBackground() {
        scope.launch(cityXploreDispatchers.io) {
            poiRepository.refreshPois()
                .onSuccess {
                    cacheManager.markAsFresh(CacheKey.POIS)
                }
            // Silently ignore errors during background refresh
        }
    }

    /**
     * Toggles the favorite status of a POI.
     * The update is optimistic - UI updates immediately via Room Flow.
     * On error, the change will be reverted by Room Flow on the next sync.
     *
     * @param poi The POI to toggle favorite status for.
     */
    fun toggleFavorite(poi: MapPoi) {
        scope.launch(cityXploreDispatchers.io) {
            toggleFavoriteUseCase(poi.id)
                .onFailure { error ->
                    // Log error but don't show to user - optimistic update will be reverted on sync
                    println("Failed to toggle favorite: ${error.message}")
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

    /**
     * Clears any error state.
     */
    fun clearError() {
        _error.value = null
    }
}
