package app.cityxplore.core

import androidx.compose.runtime.Composable

interface AvatarPicker {
    fun launch()
}

@Composable
expect fun rememberAvatarPicker(onImagePicked: (ByteArray?) -> Unit): AvatarPicker
