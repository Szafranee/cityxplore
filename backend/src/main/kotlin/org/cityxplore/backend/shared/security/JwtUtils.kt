package org.cityxplore.backend.shared.security

import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * Utility object providing helper functions for working with JSON Web Tokens (JWTs).
 */
object JwtUtils {

    /**
     * Extracts the user ID from the "sub" claim of a JSON Web Token (JWT).
     *
     * The method validates the "sub" claim, ensuring it is present and correctly formatted as a UUID.
     * If the claim is missing or invalid, an `OAuth2AuthenticationException` is thrown.
     *
     * @param jwt the JSON Web Token (JWT) from which the user ID is extracted.
     * @return the user ID extracted from the "sub" claim as a `UUID`.
     * @throws OAuth2AuthenticationException if the "sub" claim is missing or invalid.
     */
    fun extractUserId(jwt: Jwt): UUID {
        val rawSub = jwt.subject
            ?: throw OAuth2AuthenticationException(OAuth2Error("invalid_token", "Missing 'sub' claim", null))
        return try {
            UUID.fromString(rawSub)
        } catch (e: IllegalArgumentException) {
            throw OAuth2AuthenticationException(
                OAuth2Error(
                    "invalid_token",
                    "Invalid 'sub' claim: expected UUID",
                    null
                ), e
            )
        }
    }
}
