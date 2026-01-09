package org.cityxplore.backend.user.controller

import jakarta.validation.Valid
import org.cityxplore.backend.shared.security.JwtUtils
import org.cityxplore.backend.user.dto.UpdateUserProfileRequest
import org.cityxplore.backend.user.dto.UserProfileResponse
import org.cityxplore.backend.user.service.UserProfileService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller responsible for authenticated user profile operations.
 *
 * Mirrors project standards: JWT-based user id resolution, DTOs and explicit response codes.
 */
@RestController
@RequestMapping("/api/users")
@Validated
class UserProfileController(
    private val userProfileService: UserProfileService
) {

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param jwt the JSON Web Token (JWT) containing authentication and user information
     * @return the profile information of the authenticated user encapsulated in a UserProfileResponse
     */
    @GetMapping("/me")
    fun getMyProfile(@AuthenticationPrincipal jwt: Jwt): UserProfileResponse {
        val userId = JwtUtils.extractUserId(jwt)
        return userProfileService.getUserProfile(userId)
    }

    /**
     * Updates the profile of the currently authenticated user.
     *
     * This method allows an authenticated user to modify their profile details,
     * such as their username and avatar URL. The updates are applied to the user
     * identified by the JWT token provided in the request context.
     *
     * @param jwt the JSON Web Token (JWT) of the authenticated user, containing their user ID
     * @param patch the profile updates encapsulated in an `UpdateUserProfileRequest`, where only
     *              non-null fields will be updated
     * @return a `ResponseEntity` containing the updated user profile data as a `UserProfileResponse`
     */
    @PatchMapping("/me")
    fun updateMyProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody patch: UpdateUserProfileRequest
    ): ResponseEntity<UserProfileResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val updated = userProfileService.updateUserProfile(userId, patch)

        return ResponseEntity.ok(updated)
    }

    /**
     * Deletes the currently authenticated user's account.
     *
     * This action permanently removes the user and cascades the deletion to related data.
     *
     * @param jwt the JSON Web Token (JWT) of the authenticated user
     * @return a `ResponseEntity` with 204 No Content status
     */
    @DeleteMapping("/me")
    fun deleteMyAccount(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Unit> {
        val userId = JwtUtils.extractUserId(jwt)
        userProfileService.deleteUserAccount(userId)
        return ResponseEntity.noContent().build()
    }
}
