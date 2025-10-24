package org.cityxplore.backend.controller

import org.cityxplore.backend.dto.UpdateUserProfileDto
import org.cityxplore.backend.dto.UserProfileDto
import org.cityxplore.backend.security.JwtUtils
import org.cityxplore.backend.service.UserProfileService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
class UserProfileController(
    private val userProfileService: UserProfileService
) {

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param jwt the JSON Web Token (JWT) containing authentication and user information
     * @return the profile information of the authenticated user encapsulated in a UserProfileDto
     */
    @GetMapping("/me")
    fun getMyProfile(@AuthenticationPrincipal jwt: Jwt): UserProfileDto {
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
     * @param patch the profile updates encapsulated in an `UpdateUserProfileDto`, where only
     *              non-null fields will be updated
     * @return a `ResponseEntity` containing the updated user profile data as a `UserProfileDto`
     */
    @PatchMapping("/me")
    fun updateMyProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody patch: UpdateUserProfileDto
    ): ResponseEntity<UserProfileDto> {
        val userId = JwtUtils.extractUserId(jwt)
        val updated = userProfileService.updateUserProfile(userId, patch)
        return ResponseEntity.ok(updated)
    }
}
