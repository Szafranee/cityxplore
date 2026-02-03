package app.cityxplore.core.data

import app.cityxplore.core.cache.CacheManager
import app.cityxplore.database.dao.AchievementDao
import app.cityxplore.database.dao.FogOfWarDao
import app.cityxplore.database.dao.PoiDao
import app.cityxplore.database.dao.ProfileDao
import app.cityxplore.database.dao.SyncQueueDao
import app.cityxplore.map.data.clearHexagonCache
import app.cityxplore.map.domain.UpdateFogOfWarUseCase

/**
 * Service responsible for clearing all local cached data.
 *
 * This is used during sign-out to ensure no user data remains
 * in the local database when switching accounts.
 *
 * **Critical for security and data integrity:**
 * - Prevents data leakage between different user accounts
 * - Ensures fresh data is loaded for the new user after sign-in
 * - Clears any pending sync operations that belong to the previous user
 * - Invalidates all cache timestamps to force fresh data loads
 */
class LocalDataCleaner(
    private val profileDao: ProfileDao,
    private val poiDao: PoiDao,
    private val fogOfWarDao: FogOfWarDao,
    private val achievementDao: AchievementDao,
    private val syncQueueDao: SyncQueueDao,
    private val cacheManager: CacheManager,
    private val updateFogOfWarUseCase: UpdateFogOfWarUseCase
) {
    /**
     * Clears all local cached data from all tables and resets cache timestamps.
     *
     * This should be called during sign-out before the actual
     * Supabase sign-out to ensure all user-specific data is removed.
     *
     * The order of clearing is important - the sync queue should be cleared
     * first to prevent any pending operations from being executed after
     * the user has signed out.
     */
    suspend fun clearAllUserData() {
        // Clear the sync queue first to prevent pending operations from executing
        syncQueueDao.clearAll()

        // Clear user-specific data
        profileDao.clearAll()
        poiDao.clearAll()
        fogOfWarDao.clearAll()

        // Clear achievements - only user achievements, keep definitions
        achievementDao.clearAllUserAchievements()

        // Clear in-memory hexagon cache (Warsaw hexagons etc.)
        clearHexagonCache()

        // Clear UseCase internal cache
        updateFogOfWarUseCase.clearCache()

        // Clear cache timestamps to force fresh data on the next login
        cacheManager.clearAll()

        println("LocalDataCleaner: All local user data cleared successfully")
    }
}
