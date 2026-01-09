package app.cityxplore.journal.domain

import app.cityxplore.map.data.PoiRepository

class ToggleFavoriteUseCase(
    private val poiRepository: PoiRepository
) {
    suspend operator fun invoke(poiId: String): Result<Unit> {
        return poiRepository.toggleFavorite(poiId)
    }
}
