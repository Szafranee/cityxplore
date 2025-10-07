package org.cityxplore.backend.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.JWKSourceBuilder.create
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI

@Component
class JwtJwkProvider(
    @Value("\${supabase.project-ref}") private val projectRef: String
) {
    private val jwksUrl = "https://$projectRef.supabase.co/auth/v1/.well-known/jwks.json"

    fun jwtProcessor(): ConfigurableJWTProcessor<SecurityContext> {
        val proc = DefaultJWTProcessor<SecurityContext>()

        val jwkSource: JWKSource<SecurityContext> =
            create<SecurityContext>(URI.create(jwksUrl).toURL())
                .build()

        val jwsKeySelector: JWSKeySelector<SecurityContext> =
            JWSVerificationKeySelector(JWSAlgorithm.ES256, jwkSource)

        proc.jwsKeySelector = jwsKeySelector

        return proc
    }
}