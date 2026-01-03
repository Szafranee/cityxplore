package org.cityxplore.backend.fogofwar.controller

import jakarta.validation.Valid
import org.cityxplore.backend.fogofwar.model.FogOfWarResponse
import org.cityxplore.backend.fogofwar.model.RevealHexagonsRequest
import org.cityxplore.backend.fogofwar.model.WarsawHexagonsResponse
import org.cityxplore.backend.fogofwar.service.FogOfWarService
import org.cityxplore.backend.shared.security.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

/**
 * REST controller for Fog of War feature endpoints.
 *
 * Provides access to:
 * - Warsaw region hexagons (static, cacheable)
 * - User's revealed hexagons progress
 * - Reveal new hexagons operation
 * - Clear functionality (testing/debug)
 *
 * All endpoints except /warsaw-hexagons require JWT authentication.
 */
@RestController
@RequestMapping("/api/fog-of-war")
@Validated
class FogOfWarController(
    private val service: FogOfWarService
) {

    private val logger = LoggerFactory.getLogger(FogOfWarController::class.java)

    /**
     * Returns all hexagons covering the Warsaw metropolitan area.
     *
     * This endpoint returns a static, pre-computed set of hexagons that is identical
     * for all users. Response is cached aggressively (7 days) and can be served by CDN.
     *
     * @return WarsawHexagonsResponse containing all Warsaw hexagons at resolution 10
     */
    @GetMapping("/warsaw-hexagons")
    fun getWarsawHexagons(): ResponseEntity<WarsawHexagonsResponse> {
        logger.debug("Fetching Warsaw hexagons")

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
            .body(service.getWarsawHexagons())
    }

    /**
     * Returns the authenticated user's revealed hexagons.
     *
     * @param jwt JWT token containing user authentication info
     * @return FogOfWarResponse with user's revealed hexagons and last update timestamp
     */
    @GetMapping("/me")
    fun getMyRevealedHexagons(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<FogOfWarResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        logger.debug("Fetching fog of war for user {}", userId)

        return ResponseEntity.ok(service.getUserRevealedHexagons(userId))
    }

    /**
     * Adds new hexagons to the user's revealed set.
     *
     * Uses set union-semantics - duplicate hexagons are automatically ignored.
     * This endpoint is idempotent.
     *
     * @param jwt JWT token containing user authentication info
     * @param request Request body containing hexagons to reveal
     * @return RevealHexagonsResponse with operation result
     */
    @PostMapping("/reveal")
    fun revealHexagons(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: RevealHexagonsRequest
    ): ResponseEntity<Any> {
        val userId = JwtUtils.extractUserId(jwt)

        // Validation: hexagons array must not be empty
        if (request.hexagons.isEmpty()) {
            logger.warn("User $userId attempted to reveal empty hexagons array")
            return ResponseEntity
                .badRequest()
                .body(
                    mapOf(
                        "error" to "Invalid request",
                        "details" to "hexagons array cannot be empty"
                    )
                )
        }

        // Validation: limit max hexagons per request to prevent abuse
        if (request.hexagons.size > 1000) {
            logger.warn("User $userId attempted to reveal ${request.hexagons.size} hexagons (max 1000)")
            return ResponseEntity
                .badRequest()
                .body(
                    mapOf(
                        "error" to "Invalid request",
                        "details" to "Maximum 1000 hexagons per request"
                    )
                )
        }

        // Validation: basic H3 format check (15 chars, alphanumeric)
        val invalidHexagons = request.hexagons.filter { !isValidH3Index(it) }
        if (invalidHexagons.isNotEmpty()) {
            logger.warn("User $userId provided invalid hexagon formats: ${invalidHexagons.take(5)}")
            return ResponseEntity
                .badRequest()
                .body(
                    mapOf(
                        "error" to "Invalid request",
                        "details" to "Invalid hexagon format detected"
                    )
                )
        }

        logger.info("User $userId revealing ${request.hexagons.size} hexagons")
        val response = service.revealHexagons(userId, request.hexagons)

        return ResponseEntity.ok(response)
    }

    /**
     * Clears all revealed hexagons for the authenticated user.
     *
     * This endpoint is primarily for testing and debugging purposes.
     * In production, consider adding additional authorisation checks.
     *
     * @param jwt JWT token containing user authentication info
     * @return 204 No Content on success
     */
    @DeleteMapping("/me")
    fun clearAllRevealed(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Void> {
        val userId = JwtUtils.extractUserId(jwt)
        logger.info("User $userId clearing all fog of war data")

        service.clearAllRevealed(userId)

        return ResponseEntity.noContent().build()
    }

    /**
     * Validates the H3 hexagon index format.
     *
     * H3 indices at resolution 10 are 15-character strings containing
     * hexadecimal digits (0-9, a-f).
     *
     * @param hex Hexagon index to validate
     * @return true if valid, false otherwise
     */
    private fun isValidH3Index(hex: String): Boolean {
        // Basic validation: 15 chars, alphanumeric
        return hex.length == 15 && hex.all { it.isLetterOrDigit() }
    }
}
