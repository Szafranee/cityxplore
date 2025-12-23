package app.cityxplore.di

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

fun networkModule(): Module = module {
    single {
        val supabase = get<SupabaseClient>()
        // Configure HttpClient with JWT token from Supabase Auth for backend API calls
        HttpClient(get<HttpClientEngine>()) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    explicitNulls = false
                })
            }
            install(Logging) {
                level = LogLevel.BODY
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val session = supabase.auth.currentSessionOrNull()
                        session?.accessToken?.let { BearerTokens(it, session.refreshToken) }
                    }
                    refreshTokens {
                        val session = supabase.auth.currentSessionOrNull()
                        session?.accessToken?.let { BearerTokens(it, session.refreshToken) }
                    }
                }
            }
        }
    }
}
