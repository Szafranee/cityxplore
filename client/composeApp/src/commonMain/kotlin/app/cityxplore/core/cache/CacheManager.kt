package app.cityxplore.core.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/**
 * Configuration for cache validity.
 */
data class CacheConfig(
    /** How long cached data is considered fresh (in milliseconds) */
    val freshnessThresholdMs: Long = 5 * 60 * 1000, // 5 minutes by default
    /** How long cached data is usable even if stale (in milliseconds) */
    val staleThresholdMs: Long = 30 * 60 * 1000 // 30 minutes by default
)

/**
 * Represents the state of cached data.
 */
enum class CacheState {
    /** Data is fresh and doesn't need refresh */
    FRESH,

    /** Data is stale but usable, background refresh recommended */
    STALE,

    /** Data is expired or doesn't exist, refresh required */
    EXPIRED,

    /** Cache is empty, an initial load required */
    EMPTY
}

/**
 * Manages cache timestamps and determines when data should be refreshed.
 *
 * This is a key component for the lifecycle-aware caching strategy:
 * - Prevents unnecessary API calls when returning to the app
 * - Allows background refresh for stale data
 * - Forces refresh only when data is truly expired
 *
 * Usage:
 * ```
 * val cacheManager = CacheManager()
 *
 * // Check if refresh is needed
 * when (cacheManager.getCacheState(CacheKey.POIS)) {
 *     CacheState.FRESH -> { /* use cached data */ }
 *     CacheState.STALE -> { /* use cached data, trigger background refresh */ }
 *     CacheState.EXPIRED, CacheState.EMPTY -> { /* must refresh */ }
 * }
 *
 * // After successful refresh
 * cacheManager.markAsFresh(CacheKey.POIS)
 * ```
 */
class CacheManager(
    private val config: CacheConfig = CacheConfig()
) {
    private val cacheTimestamps = mutableMapOf<CacheKey, Long>()
    private val _cacheStates = MutableStateFlow<Map<CacheKey, CacheState>>(emptyMap())

    /** Observable cache states for all keys */
    val cacheStates: StateFlow<Map<CacheKey, CacheState>> = _cacheStates.asStateFlow()

    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Gets the current cache state for a given key.
     */
    fun getCacheState(key: CacheKey): CacheState {
        val timestamp = cacheTimestamps[key] ?: return CacheState.EMPTY
        val now = currentTimeMillis()
        val age = now - timestamp

        return when {
            age < config.freshnessThresholdMs -> CacheState.FRESH
            age < config.staleThresholdMs -> CacheState.STALE
            else -> CacheState.EXPIRED
        }
    }

    /**
     * Checks if the cache for a key is valid (fresh or stale but usable).
     */
    fun isValid(key: CacheKey): Boolean {
        val state = getCacheState(key)
        return state == CacheState.FRESH || state == CacheState.STALE
    }

    /**
     * Checks if the cache for a key needs refresh (stale or expired).
     */
    fun needsRefresh(key: CacheKey): Boolean {
        val state = getCacheState(key)
        return state != CacheState.FRESH
    }

    /**
     * Checks if the cache requires immediate refresh (expired or empty).
     */
    fun requiresImmediateRefresh(key: CacheKey): Boolean {
        val state = getCacheState(key)
        return state == CacheState.EXPIRED || state == CacheState.EMPTY
    }

    /**
     * Marks a cache key as freshly updated.
     */
    fun markAsFresh(key: CacheKey) {
        cacheTimestamps[key] = currentTimeMillis()
        updateStates()
    }

    /**
     * Invalidates a specific cache key, forcing refresh on next access.
     */
    fun invalidate(key: CacheKey) {
        cacheTimestamps.remove(key)
        updateStates()
    }

    /**
     * Invalidates all cache entries.
     */
    fun invalidateAll() {
        cacheTimestamps.clear()
        updateStates()
    }

    /**
     * Gets the age of cached data in milliseconds, or null if not cached.
     */
    fun getCacheAge(key: CacheKey): Long? {
        val timestamp = cacheTimestamps[key] ?: return null
        return currentTimeMillis() - timestamp
    }

    private fun updateStates() {
        _cacheStates.value = CacheKey.entries.associateWith { getCacheState(it) }
    }
}

/**
 * Keys for different cacheable data types.
 */
enum class CacheKey {
    /** User profile data */
    PROFILE,

    /** Points of Interest */
    POIS,

    /** User's POI discoveries */
    DISCOVERIES,

    /** Fog of war hexagons */
    FOG_OF_WAR,

    /** Achievement definitions */
    ACHIEVEMENTS,

    /** User's achievement progress */
    USER_ACHIEVEMENTS,

    /** Friends list */
    FRIENDS,

    /** Rankings (global and friends) */
    RANKINGS,

    /** Shared POIs */
    SHARED_POIS
}
