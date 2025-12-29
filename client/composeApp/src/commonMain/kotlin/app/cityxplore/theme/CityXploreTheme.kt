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
    val textWhite = CityXplorePalette.TextMuted
    val white = CityXplorePalette.PureWhite
    val background = CityXplorePalette.Background
}
