package app.cityxplore.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray?) -> Unit): ImagePicker {
    return remember {
        object : ImagePicker {
            override fun launch() {
                println("ImagePicker not implemented on iOS yet.")
                // No-op for now as per AvatarPicker example
            }
        }
    }
}
