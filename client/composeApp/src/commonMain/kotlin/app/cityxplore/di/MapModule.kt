package app.cityxplore.di

import app.cityxplore.map.data.NetworkPoiRepository
import app.cityxplore.map.data.PoiRepository
import app.cityxplore.map.presentation.MapViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin dependency injection module for map-related components.
 *
 * This module provides:
 * - [PoiRepository] implementation for fetching and managing POI data
 * - [MapViewModel] for managing map screen state and POI discovery logic
 *
 * @see app.cityxplore.map.data.NetworkPoiRepository
 * @see app.cityxplore.map.presentation.MapViewModel
 */
val mapModule: Module = module {
    single<PoiRepository> {
        NetworkPoiRepository(client = get())
    }
    factory { MapViewModel(get(), get()) }
}
