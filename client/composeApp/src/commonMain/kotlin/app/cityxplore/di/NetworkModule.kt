package app.cityxplore.di

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin dependency injection module for network-related components.
 *
 * This module provides a configured [HttpClient] with:
 * - **ContentNegotiation**: JSON serialization/deserialization using kotlinx.serialization
 * - **Logging**: HTTP request/response logging for debugging
 * - **Bearer Token Authentication**: Automatically injects the current Supabase JWT token
 *   into the Authorization header for every request to the backend API
 *
 * The token injection happens dynamically on each request, ensuring support for
 * account switching and automatic token refresh.
 *
 * @see app.cityxplore.di.authModule
 */
fun networkModule(): Module = module {
    single {
        val supabase = get<SupabaseClient>()
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
        }.apply {
            // Dynamically inject the current token on EVERY request to support account switching
            requestPipeline.intercept(HttpRequestPipeline.State) {
                val session = supabase.auth.currentSessionOrNull()
                session?.accessToken?.let { token ->
                    context.header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}
