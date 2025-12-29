package app.cityxplore.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cityxplore.map.presentation.MapAction
import app.cityxplore.map.presentation.MapUiState

@Composable
actual fun CityXploreMapScreen(
    state: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier,
    onProfileClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Text("Map not yet implemented on iOS")
    }
}
