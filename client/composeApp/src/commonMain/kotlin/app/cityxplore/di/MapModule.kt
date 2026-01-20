package app.cityxplore.di

import app.cityxplore.map.data.FogOfWarRepositoryImpl
import app.cityxplore.map.data.NetworkPoiRepository
import app.cityxplore.map.data.PoiRepository
import app.cityxplore.map.domain.AutoDiscoverPoisUseCase
import app.cityxplore.map.domain.DiscoverPoiUseCase
import app.cityxplore.map.domain.FogOfWarRepository
import app.cityxplore.map.domain.GetPoisWithDiscoveriesUseCase
import app.cityxplore.map.domain.UpdateFogOfWarUseCase
import app.cityxplore.map.presentation.MapViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin dependency injection module for map-related components.
 *
 * This module provides:
 * - [PoiRepository] implementation for fetching and managing POI data
 * - [FogOfWarRepository] implementation for managing fog of war state (offline-first)
 * - Domain use cases for POI operations and fog of war
 * - [MapViewModel] for managing map screen state and POI discovery logic
 *
 * @see app.cityxplore.map.data.NetworkPoiRepository
 * @see app.cityxplore.map.data.FogOfWarRepositoryImpl
 * @see app.cityxplore.map.presentation.MapViewModel
 */
val mapModule: Module = module {
    // Repositories
    single<PoiRepository> {
        NetworkPoiRepository(
            client = get(),
            poiDao = get(),
            syncQueueManager = get()
        )
    }

    single<FogOfWarRepository> {
        FogOfWarRepositoryImpl(
            httpClient = get(),
            fogOfWarDao = get(),
            syncQueueManager = get()
        )
    }

    // Use Cases
    factory { GetPoisWithDiscoveriesUseCase(repository = get()) }
    factory { DiscoverPoiUseCase(repository = get()) }
    factory { AutoDiscoverPoisUseCase(poiRepository = get(), discoverPoiUseCase = get()) }
    factory { UpdateFogOfWarUseCase(repository = get()) }

    // ViewModel
    factory {
        MapViewModel(
            getPoisUseCase = get(),
            autoDiscoverUseCase = get(),
            updateFogOfWarUseCase = get(),
            fogOfWarRepository = get(),
            poiRepository = get(),
            locationService = get(),
            profileRepository = get(),
            toggleFavoriteUseCase = get(),
            distanceTracker = get(),
            distanceSyncRepository = get(),
            sharedPoiRepository = get(),
            cacheManager = get(),
            appLifecycleObserver = get()
        )
    }
}
