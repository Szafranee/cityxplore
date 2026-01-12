package app.cityxplore.di

import app.cityxplore.core.location.DistanceTracker
import app.cityxplore.profile.data.DistanceSyncRepositoryImpl
import app.cityxplore.profile.data.ProfileRepositoryImpl
import app.cityxplore.profile.domain.DistanceSyncRepository
import app.cityxplore.profile.domain.ProfileRepository
import app.cityxplore.profile.presentation.OnboardingViewModel
import app.cityxplore.profile.presentation.ProfileViewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module for user-profile-related components.
 *
 * This module provides:
 * - [DistanceTracker] for tracking distance travelled (singleton - shared across app)
 * - [DistanceSyncRepository] for syncing distance to the backend
 * - [ProfileRepository] implementation for managing user profile data
 * - [OnboardingViewModel] for handling new user profile creation
 * - [ProfileViewModel] for displaying user profile information
 *
 * @see app.cityxplore.profile.data.ProfileRepositoryImpl
 * @see app.cityxplore.profile.presentation.OnboardingViewModel
 * @see app.cityxplore.profile.presentation.ProfileViewModel
 */
val profileModule = module {
    single { DistanceTracker() }
    single<DistanceSyncRepository> { DistanceSyncRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    factory { OnboardingViewModel(get(), get()) }
    factory { ProfileViewModel(get(), get()) }
}
