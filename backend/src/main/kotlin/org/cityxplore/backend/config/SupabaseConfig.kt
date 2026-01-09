package org.cityxplore.backend.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SupabaseConfig {

    @Value($$"${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private lateinit var issuerUri: String

    @Value($$"${supabase.secret-key}")
    private lateinit var secretKey: String

    @Bean
    fun supabaseClient(): SupabaseClient {
        // Extract base URL from issuer-uri (e.g. "https://xyz.supabase.co/auth/v1" -> "https://xyz.supabase.co")
        // If issuer-uri is "http://127.0.0.1:54321/auth/v1", base is "http://127.0.0.1:54321"
        val supabaseUrl = issuerUri.removeSuffix("/auth/v1") // Removing /auth/v1 part standard for GoTrue

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = secretKey
        ) {
            install(Auth)
            install(Storage)
        }
    }
}
