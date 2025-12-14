package app.cityxplore.di

import app.cityxplore.core.location.IosLocationService
import app.cityxplore.core.location.LocationService
import org.koin.core.module.Module
import org.koin.dsl.module

actual val locationModule: Module = module {
    single<LocationService> { IosLocationService() }
}
