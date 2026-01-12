package app.cityxplore.map.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cityxplore.map.presentation.components.AchievementUnlockedDialog
import app.cityxplore.map.presentation.components.DiscoveryNotification
import app.cityxplore.map.presentation.components.PoiDetailsContent
import app.cityxplore.profile.domain.UserProfile
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Screen displaying the interactive map with points of interest and fog of war.
 *
 * This screen handles:
 * - Displaying the map via platform-specific implementation.
 * - Showing user profile badge overlay.
 * - Handling POI selection and displaying details via bottom sheet.
 *
 * @param state The current UI state of the map.
 * @param onAction Callback for user actions on the map.
 * @param modifier Modifier to apply to the layout.
 * @param onProfileClick Callback invoked when the profile badge is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityXploreMapScreen(
    state: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()

    // Refresh map data (POIs only) when screen enters composition (e.g. returning from another tab)
    LaunchedEffect(Unit) {
        onAction(MapAction.RefreshPois)
    }

    Box(modifier = modifier.fillMaxSize()) {
        CityMapPlatformView(
            state = state,
            onAction = onAction,
            modifier = Modifier.fillMaxSize(),
            onProfileClick = onProfileClick
        )

        // User Profile Badge (Top-Left)
        if (state is MapUiState.Ready && state.profile != null) {
            UserProfileBadge(
                profile = state.profile,
                onClick = onProfileClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        top = 16.dp,
                        start = 8.dp
                    )
            )
        }

        if (state is MapUiState.Ready && state.selectedPoi != null) {
            ModalBottomSheet(
                onDismissRequest = { onAction(MapAction.DeselectPoi) },
                sheetState = sheetState
            ) {
                PoiDetailsContent(
                    poi = state.selectedPoi,
                    onToggleFavorite = { onAction(MapAction.ToggleFavorite(it)) }
                )
            }
        }

        // Discovery notification (positioned at bottom above navigation)
        if (state is MapUiState.Ready && state.newlyDiscoveredPoiIds.isNotEmpty()) {
            DiscoveryNotification(
                discoveredPoiIds = state.newlyDiscoveredPoiIds,
                pois = state.pois,
                onViewDetails = { poiId -> onAction(MapAction.ViewDiscoveredPoi(poiId)) },
                onDismiss = { poiId -> onAction(MapAction.DismissDiscoveryNotification(poiId)) },
                onDismissAll = { onAction(MapAction.DismissAllDiscoveryNotifications) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Achievement unlock dialog (shown as modal)
        if (state is MapUiState.Ready && state.newlyUnlockedAchievements.isNotEmpty()) {
            AchievementUnlockedDialog(
                achievements = state.newlyUnlockedAchievements,
                onDismiss = { onAction(MapAction.DismissAchievementNotification) }
            )
        }
    }
}

@Composable
private fun UserProfileBadge(
    profile: UserProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (profile.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(profile.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lvl ${profile.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { profile.levelProgress },
                        modifier = Modifier
                            .width(60.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
expect fun CityMapPlatformView(
    state: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
)
