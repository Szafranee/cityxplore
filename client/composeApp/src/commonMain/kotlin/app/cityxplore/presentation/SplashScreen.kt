package app.cityxplore.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cityxplore.theme.AppColors
import cityxplore.client.composeapp.generated.resources.Res
import cityxplore.client.composeapp.generated.resources.cityxplore_logo_short
import org.jetbrains.compose.resources.painterResource

/**
 * Displays the initial splash screen shown while the application is starting
 * up, performing initialisation, or restoring the current user session.
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(Res.drawable.cityxplore_logo_short),
                contentDescription = "CityXplore Logo",
                modifier = Modifier.width(200.dp) // Adjust width as needed
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = AppColors.green,
                strokeWidth = 3.dp
            )
        }
    }
}
