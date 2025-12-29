package app.cityxplore.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

val androidNetworkModule = module {
    single<HttpClientEngine> { OkHttp.create() }
}
