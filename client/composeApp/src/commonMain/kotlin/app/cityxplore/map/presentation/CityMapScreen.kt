package app.cityxplore.map.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cityxplore.map.presentation.components.PoiDetailsContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityXploreMapScreen(
    state: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = modifier.fillMaxSize()) {
        CityMapPlatformView(
            state = state,
            onAction = onAction,
            modifier = Modifier.fillMaxSize(),
            onProfileClick = onProfileClick
        )

        if (state is MapUiState.Ready && state.selectedPoi != null) {
            ModalBottomSheet(
                onDismissRequest = { onAction(MapAction.DeselectPoi) },
                sheetState = sheetState
            ) {
                PoiDetailsContent(poi = state.selectedPoi)
            }
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
