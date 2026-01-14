package app.cityxplore.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.core.rememberAvatarPicker
import app.cityxplore.profile.domain.ProfileConstants
import app.cityxplore.profile.domain.UserProfile
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.koin.compose.koinInject

/**
 * Screen displaying the user's profile information, statistics, and achievements.
 *
 * This screen includes:
 * - User avatar, username, and level progress.
 * - Statistics cards (Distance, Discoveries, Points).
 * - Navigation to Discovery Journal (placeholder).
 * - Achievements grid with details dialogs.
 * - Account settings (Avatar update, Account deletion).
 * - Sign out functionality.
 *
 * @param onSignOut Callback invoked when the user signs out.
 * @param viewModel The ViewModel explicitly managing the profile state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onOpenJournal: () -> Unit,
    viewModel: ProfileViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAvatarEditDialog by remember { mutableStateOf(false) }
    var showAccountSettingsDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }
    var showDeleteAccountFinal by remember { mutableStateOf(false) }
    var showDeleteAccountSuccess by remember { mutableStateOf(false) }
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ProfileUpdated -> {
                    showAvatarEditDialog = false
                    showAccountSettingsDialog = false
                    snackbarHostState.showSnackbar("Profile updated successfully")
                }

                is ProfileEvent.EmailChangeInitiated -> {
                    showAccountSettingsDialog = false
                    snackbarHostState.showSnackbar(
                        "Email update initiated. Please check new email ${event.newEmail} for confirmation."
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is ProfileState.Loading -> CircularProgressIndicator()
                is ProfileState.Error -> ErrorContent(
                    message = currentState.message,
                    onRetry = { viewModel.fetchProfile() },
                    onSignOut = onSignOut
                )

                is ProfileState.Success -> {
                    ProfileContent(
                        profile = currentState.profile,
                        achievements = currentState.achievements,
                        isUpdating = currentState.isUpdating,
                        onAvatarEditClick = { showAvatarEditDialog = true },
                        onSettingsClick = { showAccountSettingsDialog = true },
                        onSignOutClick = { showSignOutConfirmation = true },
                        onJournalClick = onOpenJournal,
                        onAchievementClick = { achievement ->
                            selectedAchievement = achievement
                        }
                    )

                    if (selectedAchievement != null) {
                        AchievementDetailDialog(
                            achievement = selectedAchievement!!,
                            onDismiss = { selectedAchievement = null }
                        )
                    }

                    if (showAvatarEditDialog) {
                        val avatarPicker = rememberAvatarPicker { bytes ->
                            if (bytes != null) {
                                viewModel.updateAvatar(bytes)
                                showAvatarEditDialog = false
                            }
                        }

                        AvatarEditDialog(
                            currentAvatarUrl = currentState.profile.avatarUrl,
                            isLoading = currentState.isUpdating,
                            error = currentState.updateError,
                            onDismiss = {
                                viewModel.clearError()
                                showAvatarEditDialog = false
                            },
                            onSave = { newAvatar ->
                                viewModel.updateProfile(currentState.profile.username, newAvatar)
                            },
                            onPickImage = {
                                avatarPicker.launch()
                            }
                        )
                    }

                    if (showAccountSettingsDialog) {
                        AccountSettingsDialog(
                            currentUsername = currentState.profile.username,
                            currentEmail = currentState.profile.email,
                            isLoading = currentState.isUpdating,
                            error = currentState.updateError,
                            onDismiss = {
                                viewModel.clearError()
                                showAccountSettingsDialog = false
                            },
                            onSave = { newUsername ->
                                viewModel.updateProfile(newUsername, currentState.profile.avatarUrl)
                            },
                            onChangeEmail = { newEmail ->
                                viewModel.updateEmail(newEmail)
                            },
                            onDeleteAccount = {
                                showAccountSettingsDialog = false
                                showDeleteAccountConfirmation = true
                            }
                        )
                    }

                    if (showDeleteAccountConfirmation) {
                        DeleteAccountConfirmationDialog(
                            onConfirm = {
                                showDeleteAccountConfirmation = false
                                showDeleteAccountFinal = true
                            },
                            onDismiss = {
                                showDeleteAccountConfirmation = false
                                // Consider re-opening settings? For now, just dismiss.
                            }
                        )
                    }

                    if (showDeleteAccountFinal) {
                        DeleteAccountFinalDialog(
                            expectedUsername = currentState.profile.username,
                            isLoading = currentState.isUpdating,
                            error = currentState.updateError,
                            onConfirm = {
                                viewModel.deleteAccount(
                                    onSuccess = {
                                        showDeleteAccountFinal = false
                                        showDeleteAccountSuccess = true
                                    }
                                )
                            },
                            onDismiss = {
                                viewModel.clearError()
                                showDeleteAccountFinal = false
                            }
                        )
                    }

                    if (showDeleteAccountSuccess) {
                        AlertDialog(
                            onDismissRequest = { }, // Force the user to click OK
                            title = { Text("Account Deleted") },
                            text = { Text("Your account has been successfully deleted. We are sorry to see you go.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteAccountSuccess = false
                                        onSignOut()
                                    }
                                ) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    if (showSignOutConfirmation) {
                        SignOutConfirmationDialog(
                            onConfirm = {
                                showSignOutConfirmation = false
                                onSignOut()
                            },
                            onDismiss = { showSignOutConfirmation = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    achievements: List<Achievement>,
    isUpdating: Boolean,
    onAvatarEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onJournalClick: () -> Unit,
    onAchievementClick: (Achievement) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Settings
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Account Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Avatar Section
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (profile.avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(profile.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
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

                // Loading Overlay
                if (isUpdating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            IconButton(
                onClick = onAvatarEditClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .size(32.dp),
                enabled = !isUpdating
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Avatar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Username & Level
        Text(
            text = profile.username.ifBlank { "Unknown User" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Level ${profile.level} Explorer",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // XP Bar
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { profile.levelProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${profile.xpInCurrentLevel} / ${profile.xpNeededForNextLevel} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Distance",
                value = formatDistance(profile.totalDistance),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Discoveries",
                value = profile.totalPoisDiscovered.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Points",
                value = profile.achievementPoints.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Discovery Journal Entry
        Card(
            onClick = onJournalClick,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Discovery Journal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Review your ${profile.totalPoisDiscovered} discoveries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Achievements Section
        Text(
            text = "Achievements",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        if (achievements.isEmpty()) {
            Text(
                text = "Keep exploring to unlock achievements!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        } else {
            AchievementsGrid(achievements, onAchievementClick)
        }

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedButton(
            onClick = onSignOutClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.width(200.dp)
        ) {
            Text("Sign Out")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Adjust font size for long values
            val valueStyle = if (value.length > 6) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.titleLarge
            }

            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}

@Composable
private fun AvatarEditDialog(
    currentAvatarUrl: String?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    onPickImage: () -> Unit
) {
    var avatarUrl by remember(currentAvatarUrl) { mutableStateOf(currentAvatarUrl ?: "") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Update Avatar") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Grid of options
                Box(modifier = Modifier.height(300.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 70.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                    ) {
                        // Gallery Option
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onPickImage() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f) // Ensure square aspect ratio
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Gallery",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Text("Gallery", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Predefined Avatars
                        items(items = ProfileConstants.PREDEFINED_AVATARS) { avatar ->
                            val isSelected = avatarUrl == avatar
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f) // Ensure square aspect ratio
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        if (!isLoading) {
                                            avatarUrl = avatar
                                            onSave(avatar)
                                        }
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
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(contentAlignment = Alignment.Center) {
                TextButton(
                    onClick = { onDismiss() },
                    enabled = !isLoading
                ) {
                    Text("Close")
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun AccountSettingsDialog(
    currentUsername: String,
    currentEmail: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onChangeEmail: (String) -> Unit,
    onDeleteAccount: () -> Unit
) {
    var username by remember(currentUsername) { mutableStateOf(currentUsername) }
    var email by remember(currentEmail) { mutableStateOf(currentEmail) }
    val initialEmail = remember { currentEmail } // To track if email changed

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Account Settings") },
        text = {
            Column {
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    supportingText = {
                        if (email != initialEmail) {
                            Text("Changing email requires confirmation on both new and old adrresses.")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDeleteAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.Start),
                    enabled = !isLoading
                ) {
                    Text("Delete Account")
                }
            }
        },
        confirmButton = {
            Box(contentAlignment = Alignment.Center) {
                Button(
                    onClick = {
                        // Handle username change
                        if (username != currentUsername) {
                            onSave(username)
                        }

                        // If email changed, call email update.
                        if (email != initialEmail && email.isNotBlank()) {
                            onChangeEmail(email)
                        }
                    },
                    enabled = !isLoading && (username != currentUsername || email != initialEmail)
                ) {
                    Text("Save")
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SignOutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign Out") },
        text = { Text("Are you sure you want to sign out?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sign Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = {
            Text(
                "Are you sure you want to delete your account? This action cannot be undone and you will lose all progress and data.",
                color = MaterialTheme.colorScheme.error
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteAccountFinalDialog(
    expectedUsername: String,
    isLoading: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }
    val isMatch = usernameInput == expectedUsername

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Confirm Deletion") },
        text = {
            Column {
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    "Please type your username '$expectedUsername' to confirm deletion.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Box(contentAlignment = Alignment.Center) {
                Button(
                    onClick = onConfirm,
                    enabled = isMatch && !isLoading, // Disable if loading
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Delete")
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AchievementsGrid(achievements: List<Achievement>, onAchievementClick: (Achievement) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val displayAchievements = if (expanded) achievements else achievements.take(6) // Show 2 rows initially

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val rows = displayAchievements.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { achievement ->
                    AchievementItem(
                        achievement = achievement,
                        modifier = Modifier.weight(1f).clickable { onAchievementClick(achievement) }
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (achievements.size > 6) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (expanded) "Show Less" else "Show All (${achievements.size})")
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val saturation = if (achievement.isUnlocked) 1f else 0f
        val colorMatrix = ColorMatrix().apply { setToSaturation(saturation) }
        val alpha = if (achievement.isUnlocked) 1f else 0.5f

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        ) {
            if (achievement.iconUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(achievement.iconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = achievement.name,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = if (achievement.isUnlocked) Icons.Rounded.Star else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Bar
        val progress = if (achievement.isUnlocked) 1f else achievement.progress
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = achievement.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2
        )
    }
}

@Composable
private fun AchievementDetailDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val saturation = if (achievement.isUnlocked) 1f else 0f
                val colorMatrix = ColorMatrix().apply { setToSaturation(saturation) }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (achievement.iconUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(achievement.iconUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = achievement.name,
                            colorFilter = ColorFilter.colorMatrix(colorMatrix),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (achievement.isUnlocked) Icons.Rounded.Star else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                val progress = if (achievement.isUnlocked) 1f else achievement.progress
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (achievement.isUnlocked) "Completed" else "Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = achievement.progressFormatted.ifEmpty { "${(progress * 100).toInt()}%" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${achievement.points} Points",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        "${(meters / 1000).toString().take(4)} km"
    } else {
        "${meters.toInt()} m"
    }
}
