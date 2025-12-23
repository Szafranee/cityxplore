package app.cityxplore.di

import app.cityxplore.profile.data.ProfileRepositoryImpl
import app.cityxplore.profile.domain.ProfileRepository
import app.cityxplore.profile.presentation.OnboardingViewModel
import app.cityxplore.profile.presentation.ProfileViewModel
import org.koin.dsl.module

val profileModule = module {
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    factory { OnboardingViewModel(get(), get()) }
    factory { ProfileViewModel(get()) }
}
