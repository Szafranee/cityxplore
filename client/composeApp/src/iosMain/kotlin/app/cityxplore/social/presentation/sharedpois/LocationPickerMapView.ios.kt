package app.cityxplore.social.presentation.sharedpois

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cityxplore.theme.AppColors

/**
 * iOS implementation of LocationPickerMapView.
 * For now, shows a placeholder - actual Mapbox implementation would require UIKitView.
 */
@Composable
actual fun LocationPickerMapView(
    latitude: Double?,
    longitude: Double?,
    userLatitude: Double?,
    userLongitude: Double?,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier
) {
    // iOS placeholder - interactive map would require native UIKit integration
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2D3748),
                        Color(0xFF1A202C)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                // If user has location, use it on tap
                if (userLatitude != null && userLongitude != null) {
                    onLocationSelected(userLatitude, userLongitude)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Map icon
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (latitude != null && longitude != null) {
                // Show selected location marker
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.green.copy(alpha = 0.2f))
                        .border(2.dp, AppColors.green.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AppColors.green,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatCoord(latitude)}, ${formatCoord(longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text = "Interactive map\nnot available on iOS",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to use current location",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.green.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatCoord(value: Double): String {
    val rounded = (value * 10000).toLong() / 10000.0
    return rounded.toString()
}
