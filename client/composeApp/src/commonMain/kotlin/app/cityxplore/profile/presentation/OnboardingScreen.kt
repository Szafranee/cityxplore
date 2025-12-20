package app.cityxplore.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.koin.compose.koinInject

@Composable
fun OnboardingScreen(
    onProfileCreated: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: OnboardingViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf<String?>(null) }

    // Pre-fill username from metadata if available
    LaunchedEffect(Unit) {
        viewModel.fetchUserMetadata()
    }

    // Observe username from ViewModel if we add it there, or just let ViewModel update a state flow for initial data
    // For now, let's assume ViewModel exposes it or we fetch it here.
    // But ViewModel is better.
    // Let's update ViewModel to expose initial username.
    val initialUsername by viewModel.initialUsername.collectAsState()
    LaunchedEffect(initialUsername) {
        if (initialUsername != null && username.isBlank()) {
            username = initialUsername!!
        }
    }

    LaunchedEffect(state) {
        if (state is OnboardingState.Success) {
            onProfileCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Your Profile",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Choose an Avatar")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            val avatars = listOf(
                "https://api.dicebear.com/7.x/avataaars/png?seed=Felix",
                "https://api.dicebear.com/7.x/avataaars/png?seed=Aneka",
                "https://api.dicebear.com/7.x/avataaars/png?seed=Bob",
                "https://api.dicebear.com/7.x/avataaars/png?seed=Milo"
            )
            val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow)

            avatars.zip(colors).forEach { (avatar, color) ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (selectedAvatar == avatar) color else color.copy(alpha = 0.3f))
                        .clickable { selectedAvatar = avatar },
                    contentAlignment = Alignment.Center
                ) {
                    KamelImage(
                        resource = { asyncPainterResource(avatar) },
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { CircularProgressIndicator(modifier = Modifier.size(20.dp)) },
                        onFailure = { Icon(Icons.Default.Error, "Error") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (state is OnboardingState.Error) {
            Text(
                text = (state as OnboardingState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state is OnboardingState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.createProfile(username, selectedAvatar) },
                enabled = username.isNotBlank() && selectedAvatar != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Exploring")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onSignOut
            ) {
                Text("Sign Out")
            }
        }
    }
}
