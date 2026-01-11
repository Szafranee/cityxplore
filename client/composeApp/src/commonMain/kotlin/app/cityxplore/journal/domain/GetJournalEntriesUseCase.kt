package app.cityxplore.journal.domain

import app.cityxplore.map.domain.GetPoisWithDiscoveriesUseCase
import app.cityxplore.map.domain.MapPoi
import app.cityxplore.map.domain.toMapPoi

class GetJournalEntriesUseCase(
    private val getPoisWithDiscoveriesUseCase: GetPoisWithDiscoveriesUseCase
) {
    suspend operator fun invoke(): Result<List<MapPoi>> {
        return getPoisWithDiscoveriesUseCase()
            .map { pois ->
                pois.filter { it.discovered && it.discoveryDate != null }
                    .sortedByDescending { it.discoveryDate }
                    .map { it.toMapPoi() }
            }
    }
}
