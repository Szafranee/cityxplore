package app.cityxplore.di

import app.cityxplore.core.CityXploreDispatchers
import app.cityxplore.core.cache.CacheConfig
import app.cityxplore.core.cache.CacheManager
import app.cityxplore.core.lifecycle.AppLifecycleObserver
import app.cityxplore.core.sync.DefaultSyncOperationExecutor
import app.cityxplore.core.sync.SyncOperationExecutor
import app.cityxplore.core.sync.SyncQueueManager
import org.koin.dsl.module

/**
 * Koin dependency injection module for sync and cache components.
 *
 * This module provides:
 * - **CacheManager**: Manages cache validity and determines when data refresh is needed
 * - **AppLifecycleObserver**: Tracks app lifecycle to optimise data loading
 * - **SyncQueueManager**: Handles offline operation queuing and sync
 *
 * These components work together to implement the offline-first strategy:
 * 1. CacheManager determines if cached data is still valid
 * 2. AppLifecycleObserver prevents unnecessary reloads on quick app switches
 * 3. SyncQueueManager queues offline operations and syncs when online
 */
fun syncModule() = module {
    // Coroutine dispatchers
    single { CityXploreDispatchers() }

    // Cache configuration with custom thresholds
    single {
        CacheConfig(
            freshnessThresholdMs = 5 * 60 * 1000, // 5 minutes - data is "fresh"
            staleThresholdMs = 30 * 60 * 1000    // 30 minutes - data is usable but should refresh
        )
    }

    // Cache manager singleton
    single { CacheManager(get()) }

    // App lifecycle observer
    single { AppLifecycleObserver() }

    // Sync operation executor - uses lazy injection to break circular dependency
    single<SyncOperationExecutor> {
        DefaultSyncOperationExecutor(
            poiRepository = lazy { get() },
            fogOfWarRepository = lazy { get() },
            distanceSyncRepository = lazy { get() }
        )
    }

    // Sync queue manager
    single {
        SyncQueueManager(
            syncQueueDao = get(),
            connectivityObserver = get(),
            dispatchers = get(),
            syncExecutor = get()
        )
    }
}
