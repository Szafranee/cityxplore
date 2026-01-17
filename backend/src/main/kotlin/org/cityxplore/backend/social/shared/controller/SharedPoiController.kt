package org.cityxplore.backend.social.shared.controller

import jakarta.validation.Valid
import org.cityxplore.backend.shared.security.JwtUtils
import org.cityxplore.backend.social.shared.dto.SharePoiRequest
import org.cityxplore.backend.social.shared.dto.SharedPoiResponse
import org.cityxplore.backend.social.shared.service.SharedPoiService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

/**
 * Controller responsible for handling shared Points of Interest operations.
 * Supports sharing POIs with other users, tracking views, and retrieving
 * shared POI history.
 */
@RestController
@RequestMapping("/api/shared-pois")
@PreAuthorize("isAuthenticated()")
class SharedPoiController(
    private val sharedPoiService: SharedPoiService
) {

    /**
     * Uploads an image for a custom POI.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @param file the image file to upload
     * @return a map containing the public URL of the uploaded image
     */
    @PostMapping("/images")
    fun uploadImage(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Map<String, String>> {
        val userId = JwtUtils.extractUserId(jwt)
        val url = sharedPoiService.uploadPoiImage(userId, file)
        return ResponseEntity.ok(mapOf("url" to url))
    }

    /**
     * Shares a Point of Interest with another user.
     *
     * This endpoint allows an authenticated user to share either:
     * - An existing POI (by providing `poiId`)
     * - A custom POI (by providing `customPoi` with location and details)
     *
     * Exactly one of `poiId` or `customPoi` must be provided.
     * Users can only share POIs with accepted friends.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @param sharePoiRequest the request containing recipient ID, either poiId or customPoi, and optional message
     * @return a ResponseEntity containing the created SharedPoiResponse and Location header
     */
    @PostMapping
    fun sharePoi(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody sharePoiRequest: SharePoiRequest
    ): ResponseEntity<SharedPoiResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val created = sharedPoiService.sharePoi(userId, sharePoiRequest)

        val location = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/shared-pois/{id}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }

    /**
     * Retrieves details of a specific shared POI by its ID.
     *
     * This endpoint allows an authenticated user to retrieve details of a shared POI
     * where they are either the sharer or recipient.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @param sharedPoiId the unique identifier of the shared POI to retrieve
     * @return a ResponseEntity containing the SharedPoiResponse with details
     */
    @GetMapping("/{sharedPoiId}")
    fun getSharedPoi(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sharedPoiId: UUID
    ): ResponseEntity<SharedPoiResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val result = sharedPoiService.getSharedPoiById(userId, sharedPoiId)

        return ResponseEntity.ok(result)
    }

    /**
     * Retrieves all POIs shared by the authenticated user.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @return list of SharedPoiResponse objects representing POIs shared by the user
     */
    @GetMapping("/sent")
    fun getSharedByMe(
        @AuthenticationPrincipal jwt: Jwt
    ): List<SharedPoiResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        return sharedPoiService.getSharedByMe(userId)
    }

    /**
     * Retrieves all POIs shared to the authenticated user.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @return list of SharedPoiResponse objects representing POIs shared to the user
     */
    @GetMapping("/received")
    fun getSharedToMe(
        @AuthenticationPrincipal jwt: Jwt
    ): List<SharedPoiResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        return sharedPoiService.getSharedToMe(userId)
    }

    /**
     * Retrieves all unviewed POIs shared to the authenticated user.
     *
     * This endpoint is useful for showing notifications or badges indicating
     * the number of new shared POIs that have not been viewed yet.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @return list of SharedPoiResponse objects representing unviewed POIs
     */
    @GetMapping("/received/unviewed")
    fun getUnviewedSharedToMe(
        @AuthenticationPrincipal jwt: Jwt
    ): List<SharedPoiResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        return sharedPoiService.getUnviewedSharedToMe(userId)
    }

    /**
     * Marks a shared POI as viewed by the recipient.
     *
     * This endpoint updates the viewed timestamp for a shared POI.
     * Only the intended recipient can mark a shared POI as viewed.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @param sharedPoiId the unique identifier of the shared POI to mark as viewed
     * @return a ResponseEntity containing the updated SharedPoiResponse
     */
    @PostMapping("/{sharedPoiId}/viewed")
    fun markViewed(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sharedPoiId: UUID
    ): ResponseEntity<SharedPoiResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val result = sharedPoiService.markViewed(userId, sharedPoiId)

        return ResponseEntity.ok(result)
    }

    /**
     * Marks a shared POI as discovered by the recipient.
     *
     * This endpoint is called when the recipient gets close to the shared POI location.
     * Only the intended recipient can discover a shared POI.
     * Note: Unlike regular POI discoveries, shared POI discoveries do NOT grant XP.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @param sharedPoiId the unique identifier of the shared POI to mark as discovered
     * @return a ResponseEntity containing the updated SharedPoiResponse
     */
    @PostMapping("/{sharedPoiId}/discover")
    fun discoverSharedPoi(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sharedPoiId: UUID
    ): ResponseEntity<SharedPoiResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val result = sharedPoiService.discoverSharedPoi(userId, sharedPoiId)

        return ResponseEntity.ok(result)
    }

    /**
     * Deletes a shared POI record.
     *
     * This endpoint allows the user who shared a POI to remove it.
     * Only the original sharer can delete a shared POI record.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @param sharedPoiId the unique identifier of the shared POI to delete
     * @return a ResponseEntity with 204 No Content status
     */
    @DeleteMapping("/{sharedPoiId}")
    fun deleteSharedPoi(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable sharedPoiId: UUID
    ): ResponseEntity<Void> {
        val userId = JwtUtils.extractUserId(jwt)
        sharedPoiService.deleteSharedPoi(userId, sharedPoiId)

        return ResponseEntity.noContent().build()
    }
}
