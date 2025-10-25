package org.cityxplore.backend.shared.security

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

object JwtUtils {
    fun extractUserId(jwt: Jwt): UUID {
        val rawSub = jwt.claims["sub"]?.toString()
            ?: throw IllegalArgumentException("Missing 'sub' claim in JWT")

        return try {
            UUID.fromString(rawSub)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid 'sub' claim format: expected UUID", e)
        }
    }
}
