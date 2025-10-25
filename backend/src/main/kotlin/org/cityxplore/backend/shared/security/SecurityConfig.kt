package org.cityxplore.backend.shared.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpHeaders
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.client.RestTemplate
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Security configuration for the API.
 *
 * Key points:
 * - Stateless resource server using JWT (Supabase ES256).
 * - Public endpoints under /api/public, admin endpoints under /api/admin.
 * - Custom audience and issuer validators.
 * - Role mapping: claim "role" from JWT is mapped to Spring authorities (ROLE_*)
 *   so that hasRole("ADMIN") works as expected.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private lateinit var jwkSetUri: String

    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private lateinit var issuerUri: String

    @Value("\${supabase.api-key}")
    private var supabaseApiKey: String = ""

    /**
     * Comma-separated list of allowed CORS origins. Use '*' for any origin (credentials disabled).
     */
    @Value("\${app.cors.allowed-origins:*}")
    private var allowedOrigins: String = "*"

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(BearerTokenAuthenticationEntryPoint())
            }
            .build()
    }

    /**
     * Maps JWT claim "role" to Spring Security authorities.
     * Example: role: "admin" -> authority: "ROLE_ADMIN".
     */
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        val authoritiesConverter = Converter<Jwt, Collection<GrantedAuthority>> { jwt ->
            val role = jwt.claims["role"] as? String
            if (role.isNullOrBlank()) emptyList()
            else listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
        }
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        return converter
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val builder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
            .jwsAlgorithm(SignatureAlgorithm.ES256)

        // Attach apikey/Authorization headers when fetching JWKS from Supabase if provided
        val apiKey = supabaseApiKey.trim()
        if (apiKey.isNotEmpty()) {
            val restTemplate = RestTemplate()
            restTemplate.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
                request.headers.add("apikey", apiKey)
                if (!request.headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    request.headers.add(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                }
                execution.execute(request, body)
            })
            builder.restOperations(restTemplate)
        }

        val decoder = builder.build()

        // Enforce issuer and audience as per Supabase recommendations
        val withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri)
        val audienceValidator = OAuth2TokenValidator<Jwt> { jwt ->
            val aud = jwt.audience
            if (aud.contains("authenticated")) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error("invalid_token", "Missing required audience 'authenticated'", null)
                )
            }
        }
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(withIssuer, audienceValidator))

        return decoder
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Compute allowed origins and credentials policy
        val origins = allowedOrigins.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val anyOrigin = origins.size == 1 && origins[0] == "*"
        if (anyOrigin) {
            configuration.allowedOriginPatterns = listOf("*")
            configuration.allowCredentials = false // wildcard + credentials is not allowed by browsers
        } else {
            configuration.allowedOrigins = origins
            configuration.allowCredentials = true
        }

        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Cache-Control", "Content-Type")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
