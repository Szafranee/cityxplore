package org.cityxplore.backend.achievements.controller

import org.cityxplore.backend.achievements.dto.AchievementResponse
import org.cityxplore.backend.achievements.dto.UserAchievementResponse
import org.cityxplore.backend.achievements.service.AchievementService
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
 * REST controller managing achievement-related endpoints.
 *
 * Provides public listing of available achievements and authenticated operations
 * for fetching and granting a user's achievements. Authentication is required for
 * endpoints under "/mine" and grant operations.
 */
@RestController
@RequestMapping("/api/achievements")
class AchievementController(
    private val achievementService: AchievementService
) {

    /**
     * Retrieve all available achievements.
     *
     * @param
     * @return A list of AchievementResponse representing every achievement defined in the system.
     */
    @GetMapping
    fun getAllAchievements(@AuthenticationPrincipal jwt: Jwt): List<AchievementResponse> {
        JwtUtils.extractUserId(jwt) // Validates that user is authenticated

        return achievementService.getAllAchievements()
    }

    /**
     * Retrieves the achievements for the authenticated user.
     *
     * @param jwt The authenticated user's JWT from which the user ID is extracted.
     * @return A list of UserAchievementResponse belonging to the authenticated user.
     */
    @GetMapping("/mine")
    fun getUserAchievements(@AuthenticationPrincipal jwt: Jwt): List<UserAchievementResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        return achievementService.getUserAchievements(userId)
    }

    /**
     * Retrieves the achievements for a specific user by their ID.
     * Requires authentication - only authenticated users can view other users' achievements.
     *
     * @param jwt The authenticated user's JWT (required for authorization).
     * @param userId The UUID of the user whose achievements to retrieve.
     * @return A list of UserAchievementResponse belonging to the specified user.
     */
    @GetMapping("/user/{userId}")
    fun getUserAchievementsByUserId(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable userId: UUID
    ): List<UserAchievementResponse> {
        // User must be authenticated to view other users' achievements
        JwtUtils.extractUserId(jwt) // Validates that user is authenticated

        return achievementService.getUserAchievements(userId)
    }


    /**
     * Retrieves the achievement details of a specific achievement for the authenticated user.
     *
     * @param achievementId The UUID of the specific achievement to retrieve details for.
     * @param jwt The authenticated user's JWT from which the user ID is extracted.
     * @return A ResponseEntity containing the UserAchievementResponse if the achievement exists for the user,
     *         or a ResponseEntity with a 404 status if the achievement is not found.
     */
    @GetMapping("/mine/{achievementId}")
    fun getUserAchievement(
        @PathVariable achievementId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserAchievementResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val dto = achievementService.getUserAchievement(userId, achievementId)

        return dto?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /**
     * Grants the specified achievement to the authenticated user and returns the resulting user-achievement record.
     *
     * @param achievementId The UUID of the achievement to grant.
     * @param jwt The authenticated user's JWT (used to determine the target user).
     * @return A ResponseEntity containing the created or existing UserAchievementResponse for the user.
     */
    @PostMapping("/{achievementId}/grant")
    fun grantAchievement(
        @PathVariable achievementId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserAchievementResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val result = achievementService.grantAchievement(userId, achievementId)
        val dto = result.dto

        return if (result.created) {
            ResponseEntity.created(
                ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/api/achievements/mine/{achievementId}")
                    .buildAndExpand(dto.achievement.id)
                    .toUri()
            ).body(dto)
        } else {
            ResponseEntity.ok(dto)
        }
    }
}
