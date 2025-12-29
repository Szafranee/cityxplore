package app.cityxplore.di

import app.cityxplore.core.location.AndroidLocationService
import app.cityxplore.core.location.LocationService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val locationModule: Module = module {
    single<LocationService> { AndroidLocationService(androidContext()) }
}
