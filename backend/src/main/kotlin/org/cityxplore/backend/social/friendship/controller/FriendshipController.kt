package org.cityxplore.backend.social.friendship.controller

import jakarta.validation.Valid
import org.cityxplore.backend.shared.security.JwtUtils
import org.cityxplore.backend.social.friendship.dto.FriendshipRequest
import org.cityxplore.backend.social.friendship.dto.FriendshipResponse
import org.cityxplore.backend.social.friendship.service.FriendshipService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

/**
 * Controller responsible for handling friendship-related operations.
 * Supports sending friendship invitations, accepting or declining invitations,
 * fetching a user's friends, and retrieving pending friendship requests.
 */
@RestController
@RequestMapping("/api/friends")
@PreAuthorize("isAuthenticated()")
class FriendshipController(
    private val friendshipService: FriendshipService
) {

    /**
     * Handles the creation of a friendship invitation request.
     *
     * This endpoint allows an authenticated user to send a friendship invitation to another user.
     * The user ID of the sender is extracted from the JWT, and the recipient user ID is provided
     * in the request payload. The method ensures that no conflicting state (e.g., existing friendships
     * or pending requests) exists before creating the invitation.
     *
     * @param jwt the JSON Web Token (JWT) of the authenticated user sending the invite
     * @param friendshipRequest the payload containing the addressee's ID for the friendship invitation
     * @return a ResponseEntity containing the created FriendshipResponse and the URI of the newly created resource
     */
    @PostMapping("/invite")
    fun sendInvite(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody friendshipRequest: FriendshipRequest
    ): ResponseEntity<FriendshipResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val created = friendshipService.sendInvite(userId, friendshipRequest)
        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }

    /**
     * Accepts a pending friendship invitation for the authenticated user.
     *
     * @param jwt the JSON Web Token (JWT) containing authentication details for the current user.
     * @param friendshipId the unique identifier of the friendship invitation to accept.
     * @return a ResponseEntity containing the updated FriendshipResponse,
     *         which represents the accepted friendship.
     */
    @PostMapping("/{friendshipId}/accept")
    fun acceptInvite(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable friendshipId: UUID
    ): ResponseEntity<FriendshipResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val result = friendshipService.acceptInvite(userId, friendshipId)

        return ResponseEntity.ok(result)
    }

    /**
     * Declines a pending friendship invitation for the authenticated user.
     *
     * @param jwt the JSON Web Token (JWT) representing the authenticated user's credentials
     * @param friendshipId the unique identifier of the friendship invitation to be declined
     * @return a `ResponseEntity` containing a `FriendshipResponse` that represents the declined invitation
     */
    @PostMapping("/{friendshipId}/decline")
    fun declineInvite(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable friendshipId: UUID
    ): ResponseEntity<FriendshipResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val result = friendshipService.declineInvite(userId, friendshipId)

        return ResponseEntity.ok(result)
    }

    /**
     *
     */
    @GetMapping
    fun getMyFriends(
        @AuthenticationPrincipal jwt: Jwt
    ): List<FriendshipResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        return friendshipService.getMyFriends(userId)
    }

    /**
     * Retrieves a list of pending friendship requests for the authenticated user.
     *
     * This method extracts the user ID from the provided JSON Web Token (JWT)
     * and fetches all pending friendship invitations addressed to this user.
     *
     * @param jwt the JSON Web Token (JWT) of the authenticated user.
     * @return a list of `FriendshipResponse` objects representing the pending friendship requests.
     */
    @GetMapping("/pending")
    fun getPendingRequests(
        @AuthenticationPrincipal jwt: Jwt
    ): List<FriendshipResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        return friendshipService.getPendingInvites(userId)
    }
}
