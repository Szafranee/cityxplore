package org.cityxplore.backend.social.rankings.mapper

import org.cityxplore.backend.social.rankings.dto.RankingEntryResponse
import org.cityxplore.backend.social.rankings.repository.RankingRepository

/**
 * Mapper object responsible for converting ranking entities to DTOs.
 */
object RankingMapper {

    /**
     * Converts a RankingEntry from the repository to RankingEntryResponse DTO.
     *
     * @param entry the ranking entry to convert
     * @return the corresponding RankingEntryResponse DTO
     */
    fun toResponse(entry: RankingRepository.RankingEntry): RankingEntryResponse =
        RankingEntryResponse(
            userId = entry.userId,
            username = entry.username,
            avatarUrl = entry.avatarUrl,
            score = entry.score,
            totalPoisDiscovered = entry.totalPoisDiscovered,
            totalDistance = entry.totalDistance,
            totalAchievementPoints = entry.totalAchievementPoints,
            rank = entry.rank
        )
}
