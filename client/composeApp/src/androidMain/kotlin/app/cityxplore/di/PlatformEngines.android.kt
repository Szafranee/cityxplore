package app.cityxplore.di

import org.koin.core.module.Module

actual fun providePlatformEngine(): Module = androidNetworkModule
