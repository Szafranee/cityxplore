package app.cityxplore.di

import app.cityxplore.BuildConfig
import app.cityxplore.auth.data.AuthRepositoryImpl
import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.auth.presentation.AuthViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import org.koin.dsl.module

/**
 * Koin dependency injection module for authentication-related components.
 *
 * This module provides:
 * - [AuthRepository] implementation using Supabase Auth SDK
 * - [AuthViewModel] for managing authentication state
 * - [SupabaseClient] configured with Auth, Postgrest, Storage, and Realtime plugins
 * - Supabase Auth instance for direct access if needed
 *
 * Configuration is loaded from BuildConfig, which reads from local.properties:
 * - SUPABASE_URL: The Supabase project URL
 * - SUPABASE_KEY: The Supabase anonymous/public API key
 */
val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    factory { AuthViewModel(get()) }
    single {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_KEY

        if (url == "null" || key == "null" || url.isBlank() || key.isBlank()) {
            throw IllegalStateException("Supabase URL or Key is missing. Please add SUPABASE_URL and SUPABASE_KEY to local.properties.")
        }

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Auth) {
                scheme = "app.cityxplore"
                host = "login"
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
    single { get<SupabaseClient>().auth }
}
