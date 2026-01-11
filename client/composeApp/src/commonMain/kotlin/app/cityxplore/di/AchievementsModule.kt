package app.cityxplore.di

import app.cityxplore.achievements.data.AchievementRepositoryImpl
import app.cityxplore.achievements.domain.AchievementRepository
import org.koin.dsl.module

val achievementsModule = module {
    single<AchievementRepository> { AchievementRepositoryImpl(get()) }
}
