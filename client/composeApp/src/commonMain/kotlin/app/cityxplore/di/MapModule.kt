package app.cityxplore.di

import app.cityxplore.map.data.NetworkPoiRepository
import app.cityxplore.map.data.PoiRepository
import app.cityxplore.map.presentation.MapViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val mapModule: Module = module {
    single<PoiRepository> {
        NetworkPoiRepository(client = get())
    }
    factory { MapViewModel(get(), get()) }
}
