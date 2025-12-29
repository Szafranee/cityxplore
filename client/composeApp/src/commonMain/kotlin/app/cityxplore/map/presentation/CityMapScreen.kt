package app.cityxplore.map.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CityXploreMapScreen(
    state: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
)
