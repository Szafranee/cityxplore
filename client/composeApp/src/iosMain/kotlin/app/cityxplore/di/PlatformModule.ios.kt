package app.cityxplore.di

import app.cityxplore.core.connectivity.ConnectivityObserver
import org.koin.dsl.module

actual val platformModule = module {
    includes(h3Module)

    // Connectivity observer for network status monitoring
    single { ConnectivityObserver() }
}
