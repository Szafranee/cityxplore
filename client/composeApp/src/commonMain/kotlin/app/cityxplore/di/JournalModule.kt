package app.cityxplore.di

import app.cityxplore.journal.domain.GetJournalEntriesUseCase
import app.cityxplore.journal.domain.ToggleFavoriteUseCase
import app.cityxplore.journal.presentation.JournalViewModel
import org.koin.dsl.module

val journalModule = module {
    factory { GetJournalEntriesUseCase(getPoisWithDiscoveriesUseCase = get()) }
    factory { ToggleFavoriteUseCase(poiRepository = get()) }
    factory {
        JournalViewModel(
            poiRepository = get(),
            toggleFavoriteUseCase = get(),
            cacheManager = get(),
            appLifecycleObserver = get()
        )
    }
}
