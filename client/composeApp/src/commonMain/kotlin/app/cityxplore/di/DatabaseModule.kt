package app.cityxplore.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cityxplore.core.data.LocalDataCleaner
import app.cityxplore.database.CityXploreDatabase
import app.cityxplore.database.Migrations
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
    // Uses explicit migrations to preserve data (especially SyncQueue with pending offline operations)
    single<CityXploreDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*Migrations.ALL_MIGRATIONS)
            .build()
    }

    // DAOs
    single<ProfileDao> { get<CityXploreDatabase>().profileDao() }
    single<PoiDao> { get<CityXploreDatabase>().poiDao() }
    single<FogOfWarDao> { get<CityXploreDatabase>().fogOfWarDao() }
    single<AchievementDao> { get<CityXploreDatabase>().achievementDao() }
    single<SyncQueueDao> { get<CityXploreDatabase>().syncQueueDao() }

    // LocalDataCleaner - used to clear all local data on sign-out
    single<LocalDataCleaner> {
        LocalDataCleaner(
            profileDao = get(),
            poiDao = get(),
            fogOfWarDao = get(),
            achievementDao = get(),
            syncQueueDao = get(),
            cacheManager = get(),
            updateFogOfWarUseCase = get()
        )
    }
}
