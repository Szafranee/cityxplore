package app.cityxplore.di

import app.cityxplore.social.data.repository.SocialRepositoryImpl
import app.cityxplore.social.domain.GetFriendsRankingUseCase
import app.cityxplore.social.domain.GetFriendsUseCase
import app.cityxplore.social.domain.GetGlobalRankingUseCase
import app.cityxplore.social.domain.GetPendingRequestsUseCase
import app.cityxplore.social.domain.ManageFriendshipUseCase
import app.cityxplore.social.domain.RespondToFriendInviteUseCase
import app.cityxplore.social.domain.SendFriendInviteUseCase
import app.cityxplore.social.domain.repository.SocialRepository
import app.cityxplore.social.presentation.SocialViewModel
import org.koin.dsl.module

val socialModule = module {
    // Repository
    single<SocialRepository> {
        SocialRepositoryImpl(
            client = get(),
            authRepository = get()
        )
    }

    // UseCases
    factory { GetGlobalRankingUseCase(repository = get()) }
    factory { GetFriendsRankingUseCase(repository = get()) }
    factory { GetFriendsUseCase(repository = get()) }
    factory { GetPendingRequestsUseCase(repository = get()) }
    factory { SendFriendInviteUseCase(repository = get()) }
    factory { RespondToFriendInviteUseCase(repository = get()) }
    factory { ManageFriendshipUseCase(repository = get()) }

    // ViewModel
    factory {
        SocialViewModel(
            getGlobalRankingUseCase = get(),
            getFriendsRankingUseCase = get(),
            getFriendsUseCase = get(),
            getPendingRequestsUseCase = get(),
            sendFriendInviteUseCase = get(),
            respondToFriendInviteUseCase = get(),
            manageFriendshipUseCase = get()
        )
    }
}
