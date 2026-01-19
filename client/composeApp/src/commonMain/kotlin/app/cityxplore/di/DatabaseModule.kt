package app.cityxplore.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cityxplore.database.CityXploreDatabase
import app.cityxplore.database.dao.AchievementDao
import app.cityxplore.database.dao.FogOfWarDao
import app.cityxplore.database.dao.PoiDao
import app.cityxplore.database.dao.ProfileDao
import app.cityxplore.database.dao.SyncQueueDao
import app.cityxplore.database.getDatabaseBuilder
import org.koin.dsl.module

/**
 * Koin dependency injection module for database-related components.
 *
 * This module provides:
 * - **CityXploreDatabase**: The main Room database instance
 * - **DAOs**: Data Access Objects for each entity type
 *
 * The database serves as the single source of truth for offline-first functionality,
 * caching all data locally and enabling offline access.
 *
 * Usage with Koin:
 * ```
 * val database: CityXploreDatabase = get()
 * val poiDao: PoiDao = get()
 * ```
 */
fun databaseModule() = module {
    // Main database instance (singleton)
    single<CityXploreDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // DAOs
    single<ProfileDao> { get<CityXploreDatabase>().profileDao() }
    single<PoiDao> { get<CityXploreDatabase>().poiDao() }
    single<FogOfWarDao> { get<CityXploreDatabase>().fogOfWarDao() }
    single<AchievementDao> { get<CityXploreDatabase>().achievementDao() }
    single<SyncQueueDao> { get<CityXploreDatabase>().syncQueueDao() }
}
