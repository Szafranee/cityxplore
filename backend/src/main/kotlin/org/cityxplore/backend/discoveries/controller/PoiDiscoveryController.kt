package org.cityxplore.backend.discoveries.controller

import org.cityxplore.backend.discoveries.dto.UserPoiDiscoveryResponse
import org.cityxplore.backend.discoveries.service.PoiDiscoveryService
import org.cityxplore.backend.shared.security.JwtUtils
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

/**
 * REST controller responsible for handling Point of Interest (POI) discovery operations.
 *
 * Keeps a consistent approach to authentication (JwtUtils), error semantics, and
 * DTO-based responses as in other controllers.
 */
@RestController
@RequestMapping("/api/pois")
class PoiDiscoveryController(
    private val poiDiscoveryService: PoiDiscoveryService
) {

    /**
     * Handles the discovery of a Point of Interest (POI) by a user.
     * This method verifies if the specified POI exists and checks if the user has already discovered it.
     * If the POI does not exist, a 404 NOT FOUND response is returned.
     * If the user has already discovered the POI, a 409 CONFLICT response is returned.
     * Otherwise, the discovery is saved, and the discovery data is returned with a 200 OK response.
     *
     * @param poiId the unique identifier of the Point of Interest to be discovered
     * @param jwt the JSON Web Token (JWT) containing authenticated user information
     * @return a `ResponseEntity` containing the result of the operation:
     * - 200 OK with the saved discovery DTO if successful
     * - 404 NOT FOUND if the POI does not exist
     * - 409 CONFLICT if the user has already discovered the POI
     */
    @PostMapping("/{poiId}/discover")
    fun discoverPoi(
        @PathVariable poiId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserPoiDiscoveryResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val dto = poiDiscoveryService.discoverPoi(userId, poiId)

        return ResponseEntity.created(
            ServletUriComponentsBuilder.fromCurrentRequest()
                .build()
                .toUri()
        ).body(dto)
    }

    /**
     * Retrieves the list of Points of Interest (POIs) discovered by the authenticated user.
     *
     * @param jwt the JSON Web Token (JWT) containing the authenticated user information
     * @return a list of UserPoiDiscoveryResponse objects associated with the authenticated user
     */
    @GetMapping("/discoveries")
    fun getUserDiscoveries(@AuthenticationPrincipal jwt: Jwt): List<UserPoiDiscoveryResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        return poiDiscoveryService.getUserDiscoveries(userId)
    }
}
