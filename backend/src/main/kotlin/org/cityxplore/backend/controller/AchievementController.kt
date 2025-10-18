package org.cityxplore.backend.controller

import org.cityxplore.backend.dto.AchievementDto
import org.cityxplore.backend.dto.UserAchievementDto
import org.cityxplore.backend.security.JwtUtils
import org.cityxplore.backend.service.AchievementService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/achievements")
class AchievementController(
    private val achievementService: AchievementService
) {

    @GetMapping
    fun getAllAchievements(): List<AchievementDto> =
        achievementService.getAllAchievements()

    @GetMapping("/mine")
    fun getUserAchievements(@AuthenticationPrincipal jwt: Jwt): List<UserAchievementDto> {
        val userId = JwtUtils.extractUserId(jwt)

        return achievementService.getUserAchievements(userId)
    }

    @PostMapping("/{achievementId}/grant")
    fun grantAchievement(
        @PathVariable achievementId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserAchievementDto> {
        val userId = JwtUtils.extractUserId(jwt)
        val dto = achievementService.grantAchievement(userId, achievementId)

        return ResponseEntity.ok(dto)
    }
}
