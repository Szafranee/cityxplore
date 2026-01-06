package app.cityxplore.di

import app.cityxplore.data.service.AndroidH3Service
import app.cityxplore.domain.service.H3Service
import org.koin.dsl.module

actual val h3Module = module {
    single<H3Service> { AndroidH3Service() }
}
