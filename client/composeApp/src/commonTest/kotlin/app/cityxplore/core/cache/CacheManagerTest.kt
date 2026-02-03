package app.cityxplore.core.cache

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [CacheManager].
 *
 * Checks:
 * - Cache states (Fresh, Stale, Expired, Empty)
 * - Time-based expiration logic
 * - Clearing cache
 *
 * This test uses an injected time provider to simulate time passing without waiting.
 */
class CacheManagerTest {

    private var currentTime = 1000000L // Start at an arbitrary time

    private val timeProvider = { currentTime }

    // Config: Fresh for 5 mins (300,000ms), Stale until 30 mins (1,800,000ms)
    private val config = CacheConfig(
        freshnessThresholdMs = 300_000L,
        staleThresholdMs = 1_800_000L
    )

    private val cacheManager = CacheManager(config, timeProvider)

    @Test
    fun `initial state should be EMPTY`() {
        assertEquals(CacheState.EMPTY, cacheManager.getCacheState(CacheKey.POIS))
    }

    @Test
    fun `state should be FRESH immediately after marking`() {
        cacheManager.markAsFresh(CacheKey.POIS)
        assertEquals(CacheState.FRESH, cacheManager.getCacheState(CacheKey.POIS))
    }

    @Test
    fun `state should remain FRESH within freshness threshold`() {
        cacheManager.markAsFresh(CacheKey.POIS)

        // Advance time by 4 minutes (less than the 5-min threshold)
        currentTime += 4 * 60 * 1000

        assertEquals(CacheState.FRESH, cacheManager.getCacheState(CacheKey.POIS))
    }

    @Test
    fun `state should become STALE after freshness threshold`() {
        cacheManager.markAsFresh(CacheKey.POIS)

        // Advance time by 6 minutes (more than 5 min threshold, less than 30 min)
        currentTime += 6 * 60 * 1000

        assertEquals(CacheState.STALE, cacheManager.getCacheState(CacheKey.POIS))
    }

    @Test
    fun `state should become EXPIRED after stale threshold`() {
        cacheManager.markAsFresh(CacheKey.POIS)

        // Advance time by 31 minutes (more than 30 min threshold)
        currentTime += 31 * 60 * 1000

        assertEquals(CacheState.EXPIRED, cacheManager.getCacheState(CacheKey.POIS))
    }

    @Test
    fun `clearAll should reset state to EMPTY`() {
        cacheManager.markAsFresh(CacheKey.POIS)
        assertEquals(CacheState.FRESH, cacheManager.getCacheState(CacheKey.POIS))

        cacheManager.clearAll()

        assertEquals(CacheState.EMPTY, cacheManager.getCacheState(CacheKey.POIS))
    }

    @Test
    fun `independent keys should not affect each other`() {
        cacheManager.markAsFresh(CacheKey.POIS)

        // Only POIS should be FRESH, others EMPTY
        assertEquals(CacheState.FRESH, cacheManager.getCacheState(CacheKey.POIS))
        assertEquals(CacheState.EMPTY, cacheManager.getCacheState(CacheKey.PROFILE))

        // Advance time to make POIS stale
        currentTime += 10 * 60 * 1000

        // Mark PROFILE as fresh now
        cacheManager.markAsFresh(CacheKey.PROFILE)

        assertEquals(CacheState.STALE, cacheManager.getCacheState(CacheKey.POIS))
        assertEquals(CacheState.FRESH, cacheManager.getCacheState(CacheKey.PROFILE))
    }
}
