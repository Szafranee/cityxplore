package org.cityxplore.backend.social.rankings.service

import org.cityxplore.backend.social.rankings.config.RankingConfig
import org.cityxplore.backend.social.rankings.dto.RankingEntryResponse
import org.cityxplore.backend.social.rankings.mapper.RankingMapper
import org.cityxplore.backend.social.rankings.repository.RankingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for calculating and retrieving user rankings.
 * Provides global rankings and friend-specific rankings based on user activities.
 */
@Service
class RankingService(
    private val rankingRepository: RankingRepository,
    private val rankingConfig: RankingConfig
) {

    /**
     * Retrieves the global ranking of all active users.
     *
     * The ranking is calculated based on:
     * - Total POIs discovered
     * - Total distance travelled
     * - Total achievement points earned
     *
     * Each metric is weighted according to the configuration values.
     *
     * @return list of ranking entries sorted by score (highest first)
     */
    @Transactional(readOnly = true)
    fun getGlobalRanking(): List<RankingEntryResponse> {
        val entries = rankingRepository.calculateGlobalRanking(
            poiWeight = rankingConfig.poiWeight,
            distanceWeight = rankingConfig.distanceWeight,
            achievementWeight = rankingConfig.achievementWeight
        )

        return entries.map { RankingMapper.toResponse(it) }
    }

    /**
     * Retrieves the ranking of a user and their accepted friends.
     *
     * This creates a private leaderboard showing only the user and their friends,
     * encouraging friendly competition within a user's social circle.
     *
     * @param userId the UUID of the current user
     * @return list of ranking entries for the user and their friends, sorted by score
     */
    @Transactional(readOnly = true)
    fun getFriendsRanking(userId: UUID): List<RankingEntryResponse> {
        val entries = rankingRepository.calculateFriendsRanking(
            userId = userId,
            poiWeight = rankingConfig.poiWeight,
            distanceWeight = rankingConfig.distanceWeight,
            achievementWeight = rankingConfig.achievementWeight
        )

        return entries.map { RankingMapper.toResponse(it) }
    }

    /**
     * Retrieves the user's position in the global ranking.
     *
     * This method is optimised to calculate only the rank for the requested user
     * without materialising the entire leaderboard.
     *
     * @param userId the UUID of the user
     * @return the user's ranking entry, or null if user not found or inactive
     */
    @Transactional(readOnly = true)
    fun getUserGlobalRank(userId: UUID): RankingEntryResponse? {
        val entry = rankingRepository.findGlobalRankForUser(
            userId = userId,
            poiWeight = rankingConfig.poiWeight,
            distanceWeight = rankingConfig.distanceWeight,
            achievementWeight = rankingConfig.achievementWeight
        )

        return entry?.let { RankingMapper.toResponse(it) }
    }
}
