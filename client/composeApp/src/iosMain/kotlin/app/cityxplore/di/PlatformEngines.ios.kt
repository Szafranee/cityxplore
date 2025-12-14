package app.cityxplore.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun providePlatformEngine(): Module = module {
    single<HttpClientEngine> { Darwin.create() }
}
