package app.cityxplore.di

import org.koin.dsl.module

actual val platformModule = module {
    includes(h3Module)
}
