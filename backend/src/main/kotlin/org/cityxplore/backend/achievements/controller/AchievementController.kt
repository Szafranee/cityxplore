package org.cityxplore.backend.achievements.controller

import org.cityxplore.backend.achievements.dto.AchievementDto
import org.cityxplore.backend.achievements.dto.UserAchievementDto
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
import java.net.URI
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
     * @return A list of AchievementDto representing every achievement defined in the system.
     */
    @GetMapping
    fun getAllAchievements(): List<AchievementDto> =
        achievementService.getAllAchievements()

    /**
     * Retrieves the achievements for the authenticated user.
     *
     * @param jwt The authenticated user's JWT from which the user ID is extracted.
     * @return A list of UserAchievementDto belonging to the authenticated user.
     */
    @GetMapping("/mine")
    fun getUserAchievements(@AuthenticationPrincipal jwt: Jwt): List<UserAchievementDto> {
        val userId = JwtUtils.extractUserId(jwt)

        return achievementService.getUserAchievements(userId)
    }


    /**
     * Retrieves the achievement details of a specific achievement for the authenticated user.
     *
     * @param achievementId The UUID of the specific achievement to retrieve details for.
     * @param jwt The authenticated user's JWT from which the user ID is extracted.
     * @return A ResponseEntity containing the UserAchievementDto if the achievement exists for the user,
     *         or a ResponseEntity with a 404 status if the achievement is not found.
     */
    @GetMapping("/mine/{achievementId}")
    fun getUserAchievement(
        @PathVariable achievementId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserAchievementDto> {
        val userId = JwtUtils.extractUserId(jwt)
        val dto = achievementService.getUserAchievement(userId, achievementId)

        return dto?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /**
     * Grants the specified achievement to the authenticated user and returns the resulting user-achievement record.
     *
     * @param achievementId The UUID of the achievement to grant.
     * @param jwt The authenticated user's JWT (used to determine the target user).
     * @return A ResponseEntity containing the created or existing UserAchievementDto for the user.
     */
    @PostMapping("/{achievementId}/grant")
    fun grantAchievement(
        @PathVariable achievementId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserAchievementDto> {
        val userId = JwtUtils.extractUserId(jwt)
        val result = achievementService.grantAchievement(userId, achievementId)
        val dto = result.dto

        return if (result.created) {
            ResponseEntity.created(URI.create("/api/achievements/mine/${dto.achievement.id}")).body(dto)
        } else {
            ResponseEntity.ok(dto)
        }
    }
}
