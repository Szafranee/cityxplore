package app.cityxplore.social.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cityxplore.core.location.Location
import app.cityxplore.core.ui.OfflineContent
import app.cityxplore.map.presentation.components.SharedPoiDetailsContent
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.presentation.sharedpois.SharedPoisTab
import app.cityxplore.social.presentation.sharedpois.SharedPoisUiEvent
import app.cityxplore.social.presentation.sharedpois.SharedPoisUiState
import app.cityxplore.social.presentation.sharedpois.SharedPoisViewModel
import app.cityxplore.theme.AppColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The Main screen for Social features, containing Friends, Rankings, and Shared POIs tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    initialTab: Int = 0,
    initialRankingSubTab: Int = 0,
    onUserSelected: (String, fromRankings: Boolean, isGlobalRanking: Boolean) -> Unit,
    onNavigateToMap: ((Double, Double) -> Unit)? = null,
    currentUserLatitude: Double? = null,
    currentUserLongitude: Double? = null
) {
    val viewModel: SocialViewModel = koinInject()
    val sharedPoisViewModel: SharedPoisViewModel = koinInject()
    val rankingsState by viewModel.rankingsState.collectAsState()
    val friendsState by viewModel.friendsState.collectAsState()
    val sharedPoisState by sharedPoisViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    // Renamed state to avoid conflicts
    val bottomSheetState = rememberModalBottomSheetState()

    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showCreatePoiDialog by remember { mutableStateOf(false) }

    // State for viewing shared POI details
    var currentSelectedSharedPoi by remember { mutableStateOf<SharedPoi?>(null) }
    var currentSelectedSharedPoiIsReceived by remember { mutableStateOf(false) }

    // Unviewed count for badge
    val unviewedCount = (sharedPoisState as? SharedPoisUiState.Content)?.unviewedCount ?: 0

    // Pending friend requests count for badge
    val pendingRequestsCount = (friendsState as? FriendsUiState.Content)?.pendingRequests?.size ?: 0

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is SocialUiEvent.ShowMessage -> {
                    // Cancel any existing snackbar and show the new one immediately
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        sharedPoisViewModel.uiEvents.collect { event ->
            when (event) {
                is SharedPoisUiEvent.ShowMessage -> {
                    // Cancel any existing snackbar and show the new one immediately
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }

                is SharedPoisUiEvent.NavigateToPoiOnMap -> {
                    onNavigateToMap?.invoke(event.latitude, event.longitude)
                }

                is SharedPoisUiEvent.ShareSuccess -> {
                    showCreatePoiDialog = false
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            when (pagerState.currentPage) {
                0 -> FloatingActionButton(
                    onClick = { showAddFriendDialog = true },
                    containerColor = AppColors.green
                ) { Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend") }

                2 -> FloatingActionButton(
                    onClick = { showCreatePoiDialog = true },
                    containerColor = AppColors.green
                ) { Icon(Icons.Default.AddLocationAlt, contentDescription = "Create Custom POI") }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = {
                        Box {
                            Text("Friends")
                            if (pendingRequestsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-4).dp)
                                        .size(if (pendingRequestsCount > 9) 18.dp else 16.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.red),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (pendingRequestsCount > 99) "99+" else pendingRequestsCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    })
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Rankings") })
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    text = {
                        Box {
                            Text("Shared POIs")
                            if (unviewedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-4).dp)
                                        .size(if (unviewedCount > 9) 18.dp else 16.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.red),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unviewedCount > 99) "99+" else unviewedCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    })
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Fix swipe sensitivity - make it consistent across all tabs
                beyondViewportPageCount = 1
            ) { page ->
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

                    2 -> SharedPoisTab(
                        state = sharedPoisState,
                        friendsState = friendsState,
                        createPoiState = sharedPoisViewModel.createPoiState.collectAsState().value,
                        showCreateDialog = showCreatePoiDialog,
                        onRefresh = sharedPoisViewModel::refresh,
                        onNavigate = { poi, isReceived ->
                            currentSelectedSharedPoi = poi
                            currentSelectedSharedPoiIsReceived = isReceived
                            // Mark as viewed if it's a received POI
                            if (isReceived && !poi.isViewed) {
                                sharedPoisViewModel.markAsViewed(poi)
                            }
                        },
                        onMarkViewed = sharedPoisViewModel::markAsViewed,
                        onDelete = sharedPoisViewModel::deleteSharedPoi,
                        onCreateDialogDismiss = {
                            showCreatePoiDialog = false
                            sharedPoisViewModel.resetCreatePoiState()
                        },
                        onNameChange = sharedPoisViewModel::updateCreatePoiName,
                        onDescriptionChange = sharedPoisViewModel::updateCreatePoiDescription,
                        onCategoryChange = sharedPoisViewModel::updateCreatePoiCategory,
                        onPickLocation = {
                            // Toggle location picker visibility
                            if (sharedPoisViewModel.createPoiState.value.isLocationPickerVisible) {
                                sharedPoisViewModel.hideLocationPicker()
                            } else {
                                sharedPoisViewModel.showLocationPicker()
                            }
                        },
                        onLocationPicked = sharedPoisViewModel::updateCreatePoiLocation,
                        onImagePicked = sharedPoisViewModel::updateCreatePoiImageBytes,
                        onNextStep = sharedPoisViewModel::nextStep,
                        onPreviousStep = sharedPoisViewModel::previousStep,
                        onShare = { recipientId, message ->
                            sharedPoisViewModel.shareCustomPoi(recipientId, message)
                            // Dialogue will be closed after a successful share via ShareSuccess event
                        },
                        currentUserLatitude = currentUserLatitude,
                        currentUserLongitude = currentUserLongitude
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

    // Capture the selected shared POI to a local val to prevent NPE during recomposition
    val selectedPoi = currentSelectedSharedPoi
    if (selectedPoi != null) {
        ModalBottomSheet(
            onDismissRequest = { currentSelectedSharedPoi = null },
            sheetState = bottomSheetState
        ) {
            val userLocation = if (currentUserLatitude != null && currentUserLongitude != null) {
                Location(currentUserLatitude, currentUserLongitude)
            } else null

            SharedPoiDetailsContent(
                sharedPoi = selectedPoi,
                userLocation = userLocation,
                isSentByMe = !currentSelectedSharedPoiIsReceived,
                onShowOnMap = if (currentSelectedSharedPoiIsReceived) {
                    {
                        // Capture the POI reference before closing the sheet
                        val poiToShow = selectedPoi
                        currentSelectedSharedPoi = null // Close sheet
                        sharedPoisViewModel.showPoiOnMap(poiToShow)
                    }
                } else null // Don't show the button for sent POIs
            )
        }
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

        is RankingsUiState.Error -> {
            // Check if the error is network-related (offline)
            val isOfflineError = state.message.contains("resolve host", ignoreCase = true) ||
                    state.message.contains("network", ignoreCase = true) ||
                    state.message.contains("internet", ignoreCase = true) ||
                    state.message.contains("connection", ignoreCase = true) ||
                    state.message.contains("Failed to load", ignoreCase = true)

            OfflineContent(
                title = if (isOfflineError) "You're Offline" else "Something went wrong",
                message = if (isOfflineError)
                    "Rankings requires an internet connection to load. Please check your connection and try again."
                else state.message,
                onRetry = onRefresh
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
    val authRepository: app.cityxplore.auth.domain.AuthRepository = koinInject()
    val currentUserId by androidx.compose.runtime.produceState("") {
        value = authRepository.getCurrentUserId() ?: ""
    }

    when (state) {
        is FriendsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is FriendsUiState.Error -> {
            // Check if the error is network-related (offline)
            val isOfflineError = state.message.contains("resolve host", ignoreCase = true) ||
                    state.message.contains("network", ignoreCase = true) ||
                    state.message.contains("internet", ignoreCase = true) ||
                    state.message.contains("connection", ignoreCase = true) ||
                    state.message.contains("Failed to load", ignoreCase = true)

            OfflineContent(
                title = if (isOfflineError) "You're Offline" else "Something went wrong",
                message = if (isOfflineError)
                    "Friends list requires an internet connection to load. Please check your connection and try again."
                else state.message,
                onRetry = onRefresh
            )
        }

        is FriendsUiState.Content -> {
            FriendsListContent(
                friends = state.friends,
                pendingRequests = state.pendingRequests,
                blockedUsers = state.blockedUsers,
                currentUserId = currentUserId,
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
