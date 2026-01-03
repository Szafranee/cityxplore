package org.cityxplore.backend.fogofwar.controller

import jakarta.validation.Valid
import org.cityxplore.backend.fogofwar.model.FogOfWarResponse
import org.cityxplore.backend.fogofwar.model.RevealHexagonsRequest
import org.cityxplore.backend.fogofwar.model.RevealHexagonsResponse
import org.cityxplore.backend.fogofwar.model.WarsawHexagonsResponse
import org.cityxplore.backend.fogofwar.service.FogOfWarService
import org.cityxplore.backend.shared.security.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.validation.annotation.Validated
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
     * Reveals a set of hexagons for the authenticated user.
     *
     * This operation adds the specified hexagons to the user's revealed hexagons,
     * ensuring idempotency and preventing duplication. The method logs the user's
     * activity and returns a response containing the result of the operation.
     *
     * @param jwt JWT token containing user authentication details
     * @param request Request containing the set of hexagons to reveal
     * @return ResponseEntity containing a RevealHexagonsResponse with the operation result
     */
    @PostMapping("/reveal")
    fun revealHexagons(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: RevealHexagonsRequest
    ): ResponseEntity<RevealHexagonsResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        logger.info("User $userId revealing ${request.hexagons.size} hexagons")
        val response = service.revealHexagons(userId, request.hexagons)

        return ResponseEntity.ok(response)
    }
}
