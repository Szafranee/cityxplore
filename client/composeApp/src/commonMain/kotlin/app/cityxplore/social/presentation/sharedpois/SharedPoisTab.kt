package app.cityxplore.social.presentation.sharedpois

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
    onNavigate: (SharedPoi) -> Unit,
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Error: ${state.message}\nTap to retry",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { onRefresh() }
                )
            }
        }

        is SharedPoisUiState.Content -> {
            SharedPoisContent(
                receivedPois = state.receivedPois,
                sentPois = state.sentPois,
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

        CreateCustomPoiWizard(
            state = createPoiState,
            friends = friends,
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
    onNavigate: (SharedPoi) -> Unit,
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
                isReceived = true,
                onNavigate = onNavigate,
                onMarkViewed = onMarkViewed,
                onDelete = onDelete
            )

            1 -> SharedPoiListContent(
                pois = sentPois,
                isReceived = false,
                onNavigate = onNavigate,
                onMarkViewed = onMarkViewed,
                onDelete = onDelete
            )
        }
    }
}
