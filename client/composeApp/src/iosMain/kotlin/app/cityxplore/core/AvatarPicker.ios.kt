package app.cityxplore.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAvatarPicker(onImagePicked: (ByteArray?) -> Unit): AvatarPicker {
    return remember {
        object : AvatarPicker {
            override fun launch() {
                println("AvatarPicker not implemented on iOS yet.")
                onImagePicked(null)
            }
        }
    }
}
