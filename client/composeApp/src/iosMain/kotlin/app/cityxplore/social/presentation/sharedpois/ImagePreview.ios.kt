package app.cityxplore.social.presentation.sharedpois

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun ImagePreview(
    imageBytes: ByteArray,
    modifier: Modifier
) {
    // iOS implementation placeholder - would use UIKit interop
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text("Photo selected")
    }
}
