package app.cityxplore.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
object CityXplorePalette {
    val Primary = Color(0xFF34C759)
    val Secondary = Color(0xFF03DAC5)
    val Background = Color(0xFF111111)
    val Surface = Color(0xFF181818)
    val OnPrimary = Color(0xFF0A0A0A)
    val OnBackground = Color(0xFFE8E8E8)
    val OnSurface = Color(0xFFE0E0E0)
    val TextMuted = Color(0xFFAFACAC)
    val PureWhite = Color(0xFFFFFFFF)
}

private val cityXploreDarkScheme = darkColorScheme(
    primary = CityXplorePalette.Primary,
    onPrimary = CityXplorePalette.OnPrimary,
    secondary = CityXplorePalette.Secondary,
    onSecondary = CityXplorePalette.OnPrimary,
    background = CityXplorePalette.Background,
    onBackground = CityXplorePalette.OnBackground,
    surface = CityXplorePalette.Surface,
    onSurface = CityXplorePalette.OnSurface,
    tertiary = CityXplorePalette.Secondary,
)

private val cityXploreTypography = Typography()

@Composable
fun CityXploreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = cityXploreDarkScheme,
        typography = cityXploreTypography,
        content = content
    )
}

object AppColors {
    val green = CityXplorePalette.Primary
    val cyan = CityXplorePalette.Secondary
    val textWhite = CityXplorePalette.TextMuted
    val white = CityXplorePalette.PureWhite
    val background = CityXplorePalette.Background
    val gold = Color(0xffefbf04) // Gold colour for level up star
    val red = Color(0xFFFF3B30) // Error/notification red
    val orange = Color(0xFFFF9500) // Custom POI orange
    val blue = Color(0xFF007AFF) // System POI blue

    // POI-specific colors
    val majorLandmarkGold = Color(0xFFFFD700) // Gold for major landmarks
    val majorLandmarkLabel = Color(0xFFFFA000) // Darker gold for text labels
    val sharedPoiGreen = CityXplorePalette.Primary // Shared POI accent (same as primary green)

    // Status colors
    val openStatus = Color(0xFF2E7D32) // Green for "Open Now"
    val closedStatus = Color(0xFFC62828) // Red for "Closed"
}
