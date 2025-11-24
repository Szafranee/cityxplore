package org.cityxplore.backend.social.rankings.controller

import org.cityxplore.backend.shared.security.JwtUtils
import org.cityxplore.backend.social.rankings.dto.RankingEntryResponse
import org.cityxplore.backend.social.rankings.service.RankingService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller responsible for handling ranking-related endpoints.
 * Provides access to global and friend-specific leaderboards.
 */
@RestController
@RequestMapping("/api/rankings")
class RankingController(
    private val rankingService: RankingService
) {

    /**
     * Retrieves the global ranking of all active users.
     *
     * This endpoint calculates and returns a leaderboard of all users
     * based on their exploration activities (POIs discovered, distance travelled,
     * achievements earned).
     *
     * The ranking is calculated in real-time and reflects the current state
     * of user statistics.
     *
     * @return list of ranking entries sorted by score (highest first)
     */
    @GetMapping("/global")
    @PreAuthorize("isAuthenticated()")
    fun getGlobalRanking(): List<RankingEntryResponse> {
        return rankingService.getGlobalRanking()
    }

    /**
     * Retrieves the ranking of the authenticated user and their friends.
     *
     * This endpoint creates a private leaderboard showing only the user
     * and their accepted friends, encouraging friendly competition within
     * a user's social circle.
     *
     * Only accepted friendships (status = ACCEPTED) are included in the ranking.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @return list of ranking entries for the user and their friends
     */
    @GetMapping("/friends")
    @PreAuthorize("isAuthenticated()")
    fun getFriendsRanking(
        @AuthenticationPrincipal jwt: Jwt
    ): List<RankingEntryResponse> {
        val userId = JwtUtils.extractUserId(jwt)

        return rankingService.getFriendsRanking(userId)
    }

    /**
     * Retrieves the authenticated user's position in the global ranking.
     *
     * This endpoint is useful for quickly showing the user their global rank
     * without fetching the entire leaderboard.
     *
     * @param jwt the JSON Web Token of the authenticated user
     * @return the user's ranking entry with their position and stats
     */
    @GetMapping("/global/me")
    @PreAuthorize("isAuthenticated()")
    fun getMyGlobalRank(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<RankingEntryResponse> {
        val userId = JwtUtils.extractUserId(jwt)
        val entry = rankingService.getUserGlobalRank(userId)

        return if (entry != null) {
            ResponseEntity.ok(entry)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
