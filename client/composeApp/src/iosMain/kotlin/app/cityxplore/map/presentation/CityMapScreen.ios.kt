package app.cityxplore.map.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun CityMapPlatformView(
    state: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier,
    onProfileClick: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Map not yet implemented on iOS")
    }
}
