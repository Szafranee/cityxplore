package org.cityxplore.backend.shared.security

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * Utility object providing helper functions for working with JSON Web Tokens (JWTs).
 */
object JwtUtils {
    /**
     * Extracts the user ID (UUID) from the "sub" claim of a JSON Web Token (JWT).
     *
     * The method retrieves the "sub" claim as a string and converts it to a UUID. If the "sub" claim
     * is missing or not formatted as a valid UUID, an IllegalArgumentException is thrown.
     *
     * @param jwt the JWT from which the "sub" claim will be extracted.
     * @return the extracted user ID as a UUID.
     * @throws IllegalArgumentException if the "sub" claim is missing or not a valid UUID.
     */
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
