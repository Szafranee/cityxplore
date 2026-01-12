package app.cityxplore.social.domain

import app.cityxplore.social.domain.model.RankingEntry
import app.cityxplore.social.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow

class GetFriendsRankingUseCase(private val repository: SocialRepository) {
    operator fun invoke(): Flow<List<RankingEntry>> = repository.getFriendsRanking()

    suspend fun refresh(): Result<Unit> = repository.refreshFriendsRanking()
}
