package app.cityxplore.social.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.cityxplore.theme.AppColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Main screen for Social features, containing Rankings and Friends tabs.
 */
@Composable
fun SocialScreen(
    initialTab: Int = 0,
    initialRankingSubTab: Int = 0,
    onUserSelected: (String, fromRankings: Boolean, isGlobalRanking: Boolean) -> Unit
) {
    val viewModel: SocialViewModel = koinInject()
    val rankingsState by viewModel.rankingsState.collectAsState()
    val friendsState by viewModel.friendsState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is SocialUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    var showAddFriendDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (pagerState.currentPage == 0) {
                FloatingActionButton(
                    onClick = { showAddFriendDialog = true },
                    containerColor = AppColors.green
                ) { Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend") }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Friends") })
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Rankings") })
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> FriendsTab(
                        state = friendsState,
                        onRefresh = viewModel::refreshFriends,
                        onAccept = viewModel::acceptInvite,
                        onDecline = viewModel::declineInvite,
                        onDelete = viewModel::deleteFriend,
                        onBlock = viewModel::blockFriend,
                        onUnblock = viewModel::unblockFriend,
                        onUserSelected = { userId -> onUserSelected(userId, false, false) }
                    )

                    1 -> RankingsTab(
                        state = rankingsState,
                        onRefresh = viewModel::refreshRankings,
                        initialSubTab = initialRankingSubTab,
                        onUserSelected = { userId, isGlobalRanking ->
                            onUserSelected(userId, true, isGlobalRanking)
                        }
                    )
                }
            }
        }
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onSendInvite = { username ->
                viewModel.sendInvite(username)
                showAddFriendDialog = false
            }
        )
    }
}

@Composable
fun RankingsTab(
    state: RankingsUiState,
    onRefresh: () -> Unit,
    initialSubTab: Int = 0,
    onUserSelected: (String, isGlobalRanking: Boolean) -> Unit
) {
    when (state) {
        is RankingsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is RankingsUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Error: ${state.message}\nTap to retry",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { onRefresh() }
            )
        }

        is RankingsUiState.Content -> {
            RankingListContent(state.global, state.friends, initialSubTab, onUserSelected)
        }
    }
}

@Composable
fun FriendsTab(
    state: FriendsUiState,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onUserSelected: (String) -> Unit
) {
    when (state) {
        is FriendsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is FriendsUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Error: ${state.message}\nTap to retry",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { onRefresh() }
            )
        }

        is FriendsUiState.Content -> {
            FriendsListContent(
                friends = state.friends,
                pendingRequests = state.pendingRequests,
                blockedUsers = state.blockedUsers,
                onAccept = onAccept,
                onDecline = onDecline,
                onDelete = onDelete,
                onBlock = onBlock,
                onUnblock = onUnblock,
                onUserSelected = onUserSelected
            )
        }
    }
}
