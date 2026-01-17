package app.cityxplore.core

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray?) -> Unit): ImagePicker {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use {
                            it.readBytes()
                        }
                    }
                    onImagePicked(bytes)
                }
            }
            // If uri is null (user cancelled), we don't necessarily want to trigger onImagePicked(null)
            // as it might clear existing selection if specific logic is not in place.
            // But for consistency with AvatarPicker, we might want to invoke it if logic depends on it.
            // AvatarPicker invoked it with null. Let's do nothing if null to avoid clearing by accident unless required.
        }
    )

    return remember(launcher) {
        object : ImagePicker {
            override fun launch() {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }
}
