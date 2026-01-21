package app.cityxplore.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun providePlatformEngine(): Module = module {
    includes(androidNetworkModule, androidNotificationModule())
}
