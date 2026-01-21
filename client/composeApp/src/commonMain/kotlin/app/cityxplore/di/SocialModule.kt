package app.cityxplore.di

import app.cityxplore.social.data.repository.SharedPoiRepositoryImpl
import app.cityxplore.social.data.repository.SocialRepositoryImpl
import app.cityxplore.social.domain.DeleteSharedPoiUseCase
import app.cityxplore.social.domain.GetBlockedUsersUseCase
import app.cityxplore.social.domain.GetFriendsRankingUseCase
import app.cityxplore.social.domain.GetFriendsUseCase
import app.cityxplore.social.domain.GetGlobalRankingUseCase
import app.cityxplore.social.domain.GetPendingRequestsUseCase
import app.cityxplore.social.domain.GetReceivedSharedPoisUseCase
import app.cityxplore.social.domain.GetSentSharedPoisUseCase
import app.cityxplore.social.domain.GetUnviewedSharedPoisUseCase
import app.cityxplore.social.domain.ManageFriendshipUseCase
import app.cityxplore.social.domain.MarkSharedPoiViewedUseCase
import app.cityxplore.social.domain.RespondToFriendInviteUseCase
import app.cityxplore.social.domain.SendFriendInviteUseCase
import app.cityxplore.social.domain.SharePoiUseCase
import app.cityxplore.social.domain.repository.SharedPoiRepository
import app.cityxplore.social.domain.repository.SocialRepository
import app.cityxplore.social.presentation.SocialViewModel
import app.cityxplore.social.presentation.sharedpois.SharedPoisViewModel
import org.koin.dsl.module

val socialModule = module {
    // Repositories
    single<SocialRepository> {
        SocialRepositoryImpl(
            client = get(),
            authRepository = get()
        )
    }

    single<SharedPoiRepository> {
        SharedPoiRepositoryImpl(
            client = get()
        )
    }

    // UseCases - Friends & Rankings
    factory { GetGlobalRankingUseCase(repository = get()) }
    factory { GetFriendsRankingUseCase(repository = get()) }
    factory { GetFriendsUseCase(repository = get()) }
    factory { GetPendingRequestsUseCase(repository = get()) }
    factory { GetBlockedUsersUseCase(repository = get()) }
    factory { SendFriendInviteUseCase(repository = get()) }
    factory { RespondToFriendInviteUseCase(repository = get()) }
    factory { ManageFriendshipUseCase(repository = get()) }

    // UseCases - Shared POIs
    factory { SharePoiUseCase(repository = get()) }
    factory { GetSentSharedPoisUseCase(repository = get()) }
    factory { GetReceivedSharedPoisUseCase(repository = get()) }
    factory { GetUnviewedSharedPoisUseCase(repository = get()) }
    factory { MarkSharedPoiViewedUseCase(repository = get()) }
    factory { DeleteSharedPoiUseCase(repository = get()) }

    // ViewModels
    factory {
        SocialViewModel(
            getGlobalRankingUseCase = get(),
            getFriendsRankingUseCase = get(),
            getFriendsUseCase = get(),
            getPendingRequestsUseCase = get(),
            getBlockedUsersUseCase = get(),
            sendFriendInviteUseCase = get(),
            respondToFriendInviteUseCase = get(),
            manageFriendshipUseCase = get(),
            connectivityObserver = get(),
            socialNotificationManager = get()
        )
    }

    factory {
        SharedPoisViewModel(
            getSentSharedPoisUseCase = get(),
            getReceivedSharedPoisUseCase = get(),
            getUnviewedSharedPoisUseCase = get(),
            sharePoiUseCase = get(),
            markSharedPoiViewedUseCase = get(),
            deleteSharedPoiUseCase = get(),
            sharedPoiRepository = get(),
            appLifecycleObserver = get(),
            connectivityObserver = get(),
            socialNotificationManager = get()
        )
    }
}
