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
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
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
        }
    }
    single { get<SupabaseClient>().auth }
}
