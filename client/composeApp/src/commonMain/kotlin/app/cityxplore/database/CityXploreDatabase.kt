package app.cityxplore.database

import androidx.room.Database
import androidx.room.RoomDatabase
import app.cityxplore.database.dao.AchievementDao
import app.cityxplore.database.dao.FogOfWarDao
import app.cityxplore.database.dao.PoiDao
import app.cityxplore.database.dao.ProfileDao
import app.cityxplore.database.dao.SyncQueueDao
import app.cityxplore.database.entity.AchievementEntity
import app.cityxplore.database.entity.FogOfWarEntity
import app.cityxplore.database.entity.PoiEntity
import app.cityxplore.database.entity.SyncQueueEntity
import app.cityxplore.database.entity.UserAchievementEntity
import app.cityxplore.database.entity.UserProfileEntity

/**
 * Main Room database for CityXplore offline-first functionality.
 *
 * This database serves as the single source of truth for all cached data,
 * enabling offline access and reducing unnecessary network requests.
 *
 * Tables:
 * - user_profiles: Current user's profile data
 * - pois: Points of Interest with discovery/favorite status
 * - fog_of_war: Revealed H3 hexagons
 * - achievements: Achievement definitions
 * - user_achievements: User's achievement progress
 * - sync_queue: Pending offline operations
 */
@Database(
    entities = [
        UserProfileEntity::class,
        PoiEntity::class,
        FogOfWarEntity::class,
        AchievementEntity::class,
        UserAchievementEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class CityXploreDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun poiDao(): PoiDao
    abstract fun fogOfWarDao(): FogOfWarDao
    abstract fun achievementDao(): AchievementDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val DATABASE_NAME = "cityxplore.db"
    }
}
