package app.cityxplore.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.cityxplore.core.rememberAvatarPicker
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.koin.compose.koinInject

@Composable
fun OnboardingScreen(
    onProfileCreated: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: OnboardingViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    var username by remember { mutableStateOf("") }
    // Holds either a String (URL) or ByteArray (Local Image)
    var selectedAvatar by remember { mutableStateOf<Any?>(null) }

    val avatarPicker = rememberAvatarPicker { bytes ->
        if (bytes != null) {
            selectedAvatar = bytes
            viewModel.setAvatar(bytes)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchUserMetadata()
    }

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

        // Selected Avatar Preview (Big)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selectedAvatar != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(selectedAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Show loading if uploading a custom avatar
            if (uploadState is AvatarUploadState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            isError = state is OnboardingState.Error
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Choose Avatar", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Infinite/Long Row of Options
        // Use a fading edge or content padding to indicate scrolling
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            // Content padding ensures the first/last items aren't stuck to the edge
            // and allows the next item to 'peek'
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            item {
                // Gallery Picker Button
                // Fixed width container to prevent layout shifting
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(70.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { avatarPicker.launch() }
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Upload from Gallery",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Gallery",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }

            items(items = viewModel.predefinedAvatars) { avatar ->
                val isSelected = selectedAvatar == avatar
                // Wrap in Column to match Gallery button height/alignment
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(70.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                selectedAvatar = avatar
                                viewModel.setAvatar(null)
                            }
                            .then(
                                if (isSelected) Modifier.border(
                                    3.dp,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(avatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar Option",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Spacer + Empty Text to match Gallery label height visually
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "", // Placeholder or could be name
                        style = MaterialTheme.typography.labelSmall
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
                onClick = {
                    val avatarUrl = selectedAvatar as? String
                    viewModel.createProfile(username, avatarUrl)
                },
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
