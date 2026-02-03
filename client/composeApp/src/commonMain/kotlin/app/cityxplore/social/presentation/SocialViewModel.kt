package app.cityxplore.social.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cityxplore.core.connectivity.ConnectivityObserver
import app.cityxplore.core.notifications.SocialDataChangeEvent
import app.cityxplore.core.notifications.SocialNotificationManager
import app.cityxplore.social.domain.GetBlockedUsersUseCase
import app.cityxplore.social.domain.GetFriendsRankingUseCase
import app.cityxplore.social.domain.GetFriendsUseCase
import app.cityxplore.social.domain.GetGlobalRankingUseCase
import app.cityxplore.social.domain.GetPendingRequestsUseCase
import app.cityxplore.social.domain.ManageFriendshipUseCase
import app.cityxplore.social.domain.RespondToFriendInviteUseCase
import app.cityxplore.social.domain.SendFriendInviteUseCase
import app.cityxplore.social.domain.exception.CannotInviteSelfException
import app.cityxplore.social.domain.exception.UserNotFoundException
import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.model.RankingEntry
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents one-off UI events for the Social screen, such as displaying transient messages.
 */
sealed interface SocialUiEvent {
    data class ShowMessage(val message: String) : SocialUiEvent
}

/**
 * UI State for the Rankings Tab.
 */
sealed interface RankingsUiState {
    data object Loading : RankingsUiState
    data class Content(
        val global: List<RankingEntry>,
        val friends: List<RankingEntry>
    ) : RankingsUiState

    data class Error(val message: String) : RankingsUiState
}

/**
 * UI State for the Friends Management Tab.
 */
sealed interface FriendsUiState {
    data object Loading : FriendsUiState
    data class Content(
        val friends: List<Friendship>,
        val pendingRequests: List<Friendship>,
        val blockedUsers: List<Friendship>
    ) : FriendsUiState

    data class Error(val message: String) : FriendsUiState
}

/**
 * ViewModel responsible for managing the state of the Social screen (Rankings and Friends).
 *
 * It coordinates data fetching from multiple UseCases to provide a cohesive state for the UI,
 * handling separate loading states for Rankings and Friends tabs to improve UX.
 *
 * **Note:** Social features require network connectivity and do not work offline.
 * The ViewModel checks connectivity before performing social actions.
 */
class SocialViewModel(
    private val getGlobalRankingUseCase: GetGlobalRankingUseCase,
    private val getFriendsRankingUseCase: GetFriendsRankingUseCase,
    private val getFriendsUseCase: GetFriendsUseCase,
    private val getPendingRequestsUseCase: GetPendingRequestsUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val sendFriendInviteUseCase: SendFriendInviteUseCase,
    private val respondToFriendInviteUseCase: RespondToFriendInviteUseCase,
    private val manageFriendshipUseCase: ManageFriendshipUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val socialNotificationManager: SocialNotificationManager
) : ViewModel() {

    private val offlineMessage = "This feature requires an internet connection"

    // Internal mutable states to track loading/error status
    private val _isRankingsLoading = MutableStateFlow(false)
    private val _rankingsError = MutableStateFlow<String?>(null)

    /** Public StateFlow for pull-to-refresh indicator on Rankings tab */
    val isRankingsRefreshing: StateFlow<Boolean> = _isRankingsLoading

    // Channel for one-off events (e.g. Snackbar messages)
    private val _uiEvents = Channel<SocialUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    // We expose two separate states because the Social Screen has two distinct Tabs
    // with independent data requirements.

    /**
     * StateFlow representing the current state of the Rankings tab.
     * Combines global and friends rankings with loading and error states.
     */
    val rankingsState: StateFlow<RankingsUiState> = combine(
        getGlobalRankingUseCase(),
        getFriendsRankingUseCase(),
        _isRankingsLoading,
        _rankingsError
    ) { global, friendsRank, isLoading, error ->
        // We only show the Loading state if we have no cached data to display.
        // This ensures a better UX during pull-to-refresh (content remains visible).
        if (isLoading && global.isEmpty() && friendsRank.isEmpty()) {
            RankingsUiState.Loading
        } else if (error != null) {
            RankingsUiState.Error(error)
        } else {
            RankingsUiState.Content(global, friendsRank)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RankingsUiState.Loading)


    private val _isFriendsLoading = MutableStateFlow(false)
    private val _friendsError = MutableStateFlow<String?>(null)

    /** Public StateFlow for pull-to-refresh indicator on Friends tab */
    val isFriendsRefreshing: StateFlow<Boolean> = _isFriendsLoading

    /**
     * StateFlow representing the current state of the Friends tab.
     * Combines friends list, pending requests, and blocked users.
     */
    val friendsState: StateFlow<FriendsUiState> = combine(
        getFriendsUseCase(),
        getPendingRequestsUseCase(),
        getBlockedUsersUseCase(),
        _isFriendsLoading,
        _friendsError
    ) { friends, pending, blocked, isLoading, error ->
        if (isLoading && friends.isEmpty() && pending.isEmpty() && blocked.isEmpty()) {
            FriendsUiState.Loading
        } else if (error != null) {
            FriendsUiState.Error(error)
        } else {
            FriendsUiState.Content(friends, pending, blocked)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FriendsUiState.Loading)


    init {
        refreshAll()
        observeDataChanges()
    }

    /**
     * Observes real-time data change events from Supabase Realtime.
     * Automatically refreshes data when friends or rankings change.
     */
    private fun observeDataChanges() {
        viewModelScope.launch {
            socialNotificationManager.dataChangeEvents.collect { event ->
                println("SocialViewModel: Received data change event: $event")
                when (event) {
                    is SocialDataChangeEvent.FriendsChanged -> {
                        refreshFriends()
                    }

                    is SocialDataChangeEvent.RankingsChanged -> {
                        refreshRankings()
                        refreshFriends() // Also refresh friends to update wizard friend picker
                    }

                    is SocialDataChangeEvent.SharedPoisChanged -> {
                        // SharedPoisViewModel will handle this
                    }
                }
            }
        }
    }

    /**
     * Triggers a refresh of all social data (rankings and friends).
     */
    fun refreshAll() {
        refreshRankings()
        refreshFriends()
    }

    fun refreshRankings() {
        viewModelScope.launch {
            _isRankingsLoading.value = true
            _rankingsError.value = null

            val globalResult = getGlobalRankingUseCase.refresh()
            val friendsResult = getFriendsRankingUseCase.refresh()

            if (globalResult.isFailure || friendsResult.isFailure) {
                // We simplify error handling for the MVP: any failure triggers a generic error state
                _rankingsError.value = "Failed to load rankings"
            }
            _isRankingsLoading.value = false
        }
    }

    fun refreshFriends() {
        viewModelScope.launch {
            _isFriendsLoading.value = true
            _friendsError.value = null

            val friendsResult = getFriendsUseCase.refresh()
            val pendingResult = getPendingRequestsUseCase.refresh()
            val blockedResult = getBlockedUsersUseCase.refresh()

            if (friendsResult.isFailure || pendingResult.isFailure || blockedResult.isFailure) {
                _friendsError.value = "Failed to load friends"
            }
            _isFriendsLoading.value = false
        }
    }

    /**
     * Sends a friend invitation to the user with the specified username.
     * Requires network connectivity.
     *
     * @param username The exact username of the user to invite.
     */
    fun sendInvite(username: String) {
        viewModelScope.launch {
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.send(SocialUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            val result = sendFriendInviteUseCase(username)
            if (result.isSuccess) {
                // Refreshing friends list ensures any state changes (like pending outgoing)
                // are reflected, though currently we only track incoming requests.
                refreshFriends()
                _uiEvents.send(SocialUiEvent.ShowMessage("Invitation sent to $username"))
            } else {
                val message = when (val exception = result.exceptionOrNull()) {
                    is CannotInviteSelfException -> "You cannot send a friend request to yourself."

                    is UserNotFoundException -> "User '$username' not found."

                    is ClientRequestException -> {
                        when (exception.response.status) {
                            HttpStatusCode.Conflict -> "Invitation already sent or user is already a friend."
                            HttpStatusCode.NotFound -> "User '$username' not found."
                            else -> "Failed to send invite: ${exception.response.status.description}"
                        }
                    }

                    else -> exception?.message ?: "Failed to send invite"
                }
                _uiEvents.send(SocialUiEvent.ShowMessage(message))
            }
        }
    }

    fun acceptInvite(friendshipId: String) {
        viewModelScope.launch {
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.send(SocialUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            respondToFriendInviteUseCase.accept(friendshipId)
                .onSuccess {
                    refreshFriends()
                    // The new friend should appear in the friends ranking immediately
                    refreshRankings()
                    _uiEvents.send(SocialUiEvent.ShowMessage("Friend request accepted"))
                }
                .onFailure {
                    _uiEvents.send(SocialUiEvent.ShowMessage("Failed to accept request"))
                }
        }
    }

    fun declineInvite(friendshipId: String) {
        viewModelScope.launch {
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.send(SocialUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            respondToFriendInviteUseCase.decline(friendshipId)
                .onSuccess {
                    refreshFriends()
                    _uiEvents.send(SocialUiEvent.ShowMessage("Friend request declined"))
                }
                .onFailure {
                    _uiEvents.send(SocialUiEvent.ShowMessage("Failed to decline request"))
                }
        }
    }

    fun deleteFriend(friendshipId: String) {
        viewModelScope.launch {
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.send(SocialUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            manageFriendshipUseCase.deleteFriend(friendshipId)
                .onSuccess {
                    refreshFriends()
                    refreshRankings() // Refresh rankings as friend count may have changed
                    _uiEvents.send(SocialUiEvent.ShowMessage("Friend removed"))
                }
                .onFailure {
                    _uiEvents.send(SocialUiEvent.ShowMessage("Failed to remove friend"))
                }
        }
    }

    fun blockFriend(friendshipId: String) {
        viewModelScope.launch {
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.send(SocialUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            manageFriendshipUseCase.blockFriend(friendshipId)
                .onSuccess {
                    refreshFriends()
                    _uiEvents.send(SocialUiEvent.ShowMessage("User blocked"))
                }
                .onFailure {
                    _uiEvents.send(SocialUiEvent.ShowMessage("Failed to block user"))
                }
        }
    }

    fun unblockFriend(friendshipId: String) {
        viewModelScope.launch {
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.send(SocialUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            manageFriendshipUseCase.unblockFriend(friendshipId)
                .onSuccess {
                    refreshFriends()
                    _uiEvents.send(SocialUiEvent.ShowMessage("User unblocked"))
                }
                .onFailure {
                    _uiEvents.send(SocialUiEvent.ShowMessage("Failed to unblock user"))
                }
        }
    }
}
