package app.cityxplore.social.presentation.sharedpois

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.cityxplore.core.ui.OfflineContent
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.presentation.FriendsUiState

/**
 * Tab content for Shared POIs with Received/Sent sub-tabs.
 */
@Composable
fun SharedPoisTab(
    state: SharedPoisUiState,
    friendsState: FriendsUiState,
    createPoiState: CreateCustomPoiState,
    showCreateDialog: Boolean,
    onRefresh: () -> Unit,
    onNavigate: (SharedPoi, Boolean) -> Unit, // Boolean = isReceived
    onMarkViewed: (SharedPoi) -> Unit,
    onDelete: (SharedPoi) -> Unit,
    onCreateDialogDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPickLocation: () -> Unit,
    onLocationPicked: (Double, Double) -> Unit,
    onImagePicked: (ByteArray?) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onShare: (recipientId: String, message: String?) -> Unit,
    currentUserLatitude: Double?,
    currentUserLongitude: Double?
) {
    when (state) {
        is SharedPoisUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is SharedPoisUiState.Error -> {
            // Check if the error is network-related (offline)
            val isOfflineError = state.message.contains("resolve host", ignoreCase = true) ||
                    state.message.contains("network", ignoreCase = true) ||
                    state.message.contains("internet", ignoreCase = true) ||
                    state.message.contains("connection", ignoreCase = true)

            if (isOfflineError) {
                OfflineContent(
                    title = "You're Offline",
                    message = "Shared POIs require an internet connection to load. Please check your connection and try again.",
                    onRetry = onRefresh
                )
            } else {
                OfflineContent(
                    title = "Something went wrong",
                    message = state.message,
                    onRetry = onRefresh
                )
            }
        }

        is SharedPoisUiState.Content -> {
            SharedPoisContent(
                receivedPois = state.receivedPois,
                sentPois = state.sentPois,
                deletingIds = state.deletingIds,
                onNavigate = onNavigate,
                onMarkViewed = onMarkViewed,
                onDelete = onDelete
            )
        }
    }

    // Create Custom POI Wizard (multi-step)
    if (showCreateDialog) {
        val friends = when (friendsState) {
            is FriendsUiState.Content -> friendsState.friends
            else -> emptyList()
        }

        // Calculate sent POIs per recipient
        val sentPois = (state as? SharedPoisUiState.Content)?.sentPois ?: emptyList()
        val sentPoisPerRecipient = sentPois
            .groupBy { it.recipientId }
            .mapValues { it.value.size }

        CreateCustomPoiWizard(
            state = createPoiState,
            friends = friends,
            sentPoisPerRecipient = sentPoisPerRecipient,
            onNameChange = onNameChange,
            onDescriptionChange = onDescriptionChange,
            onCategoryChange = onCategoryChange,
            onPickLocation = onPickLocation,
            onLocationPicked = onLocationPicked,
            onImagePicked = onImagePicked,
            onNextStep = onNextStep,
            onPreviousStep = onPreviousStep,
            onShare = onShare,
            onDismiss = onCreateDialogDismiss,
            currentUserLatitude = currentUserLatitude,
            currentUserLongitude = currentUserLongitude
        )
    }
}

@Composable
private fun SharedPoisContent(
    receivedPois: List<SharedPoi>,
    sentPois: List<SharedPoi>,
    deletingIds: Set<String>,
    onNavigate: (SharedPoi, Boolean) -> Unit,
    onMarkViewed: (SharedPoi) -> Unit,
    onDelete: (SharedPoi) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Received (${receivedPois.size})") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Sent (${sentPois.size})") }
            )
        }

        when (selectedTabIndex) {
            0 -> SharedPoiListContent(
                pois = receivedPois,
                deletingIds = deletingIds,
                isReceived = true,
                onNavigate = { onNavigate(it, true) },
                onMarkViewed = onMarkViewed,
                onDelete = onDelete
            )

            1 -> SharedPoiListContent(
                pois = sentPois,
                deletingIds = deletingIds,
                isReceived = false,
                onNavigate = { onNavigate(it, false) },
                onMarkViewed = onMarkViewed,
                onDelete = onDelete
            )
        }
    }
}
