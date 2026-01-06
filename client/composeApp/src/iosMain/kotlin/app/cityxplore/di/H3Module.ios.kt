package app.cityxplore.di

import app.cityxplore.data.service.IosH3Service
import app.cityxplore.domain.service.H3Service
import org.koin.dsl.module

actual val h3Module = module {
    single<H3Service> { IosH3Service() }
}
