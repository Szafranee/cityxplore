package app.cityxplore.social.presentation.sharedpois

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cityxplore.core.rememberImagePicker
import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.theme.AppColors
import coil3.compose.AsyncImage

/**
 * Multistep wizard dialogue for creating and sharing a custom POI.
 */
@Composable
fun CreateCustomPoiWizard(
    state: CreateCustomPoiState,
    friends: List<Friendship>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPickLocation: () -> Unit,
    onLocationPicked: (Double, Double) -> Unit,
    onImagePicked: (ByteArray?) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onShare: (recipientId: String, message: String?) -> Unit,
    onDismiss: () -> Unit,
    currentUserLatitude: Double?,
    currentUserLongitude: Double?
) {
    Dialog(
        onDismissRequest = { if (!state.isUploading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                WizardHeader(
                    currentStep = state.currentStep,
                    onClose = { if (!state.isUploading) onDismiss() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                    modifier = Modifier.weight(1f, fill = false)
                ) { step ->
                    when (step) {
                        CreatePoiStep.BASIC_INFO -> Step1BasicInfo(
                            state = state,
                            onNameChange = onNameChange,
                            onDescriptionChange = onDescriptionChange,
                            onCategoryChange = onCategoryChange
                        )

                        CreatePoiStep.LOCATION_PHOTO -> Step2LocationPhoto(
                            state = state,
                            onPickLocation = onPickLocation,
                            onLocationPicked = onLocationPicked,
                            onImagePicked = onImagePicked,
                            currentUserLatitude = currentUserLatitude,
                            currentUserLongitude = currentUserLongitude
                        )

                        CreatePoiStep.SELECT_FRIEND -> Step3SelectFriend(
                            state = state,
                            friends = friends,
                            onShare = onShare
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                WizardNavigation(
                    currentStep = state.currentStep,
                    isStep1Valid = state.isStep1Valid,
                    isStep2Valid = state.isStep2Valid,
                    isUploading = state.isUploading,
                    onNext = onNextStep,
                    onBack = onPreviousStep,
                    onCancel = onDismiss
                )
            }
        }
    }

    if (state.isLocationPickerVisible) {
        LocationPickerDialog(
            currentLatitude = state.latitude,
            currentLongitude = state.longitude,
            userLatitude = currentUserLatitude,
            userLongitude = currentUserLongitude,
            onDismiss = onPickLocation,
            onLocationSelected = { lat, lng ->
                onLocationPicked(lat, lng)
                onPickLocation()
            }
        )
    }
}

@Composable
private fun WizardHeader(
    currentStep: CreatePoiStep,
    onClose: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (currentStep) {
                    CreatePoiStep.BASIC_INFO -> "Create POI"
                    CreatePoiStep.LOCATION_PHOTO -> "Location & Photo"
                    CreatePoiStep.SELECT_FRIEND -> "Share With"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (currentStep.ordinal + 1) / 3f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AppColors.green
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Step ${currentStep.ordinal + 1} of 3",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step1BasicInfo(
    state: CreateCustomPoiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Name *") },
            placeholder = { Text("e.g., My Secret Spot") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("${state.name.length}/200") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            placeholder = { Text("Tell your friend about this place...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("${state.description.length}/1000") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Category *",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            customPoiCategories.forEach { (id, label) ->
                CategoryChip(
                    label = label,
                    isSelected = state.category == id,
                    onClick = { onCategoryChange(id) }
                )
            }
        }
    }
}

@Composable
private fun Step2LocationPhoto(
    state: CreateCustomPoiState,
    onPickLocation: () -> Unit,
    onLocationPicked: (Double, Double) -> Unit,
    onImagePicked: (ByteArray?) -> Unit,
    currentUserLatitude: Double?,
    currentUserLongitude: Double?
) {
    val imagePicker = rememberImagePicker(onImagePicked = onImagePicked)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Location *",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.latitude != null && state.longitude != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.green.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AppColors.green,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Location set",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.green
                    )
                    Text(
                        text = "%.4f, %.4f".format(state.latitude, state.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "No location selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickLocation,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.latitude != null) AppColors.green.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (state.latitude != null) AppColors.green else MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (state.latitude != null) "Change" else "Pick on Map",
                    color = if (state.latitude != null) AppColors.green else MaterialTheme.colorScheme.onPrimary
                )
            }

            if (currentUserLatitude != null && currentUserLongitude != null) {
                OutlinedButton(
                    onClick = { onLocationPicked(currentUserLatitude, currentUserLongitude) }
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("My Location")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Photo (optional)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.imageBytes != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePicker.launch() }
            ) {
                ImagePreview(
                    imageBytes = state.imageBytes,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { onImagePicked(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove photo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap image to change",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            OutlinedButton(
                onClick = { imagePicker.launch() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Photo")
            }
        }
    }
}

@Composable
private fun Step3SelectFriend(
    state: CreateCustomPoiState,
    friends: List<Friendship>,
    onShare: (recipientId: String, message: String?) -> Unit
) {
    var selectedFriendId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (friends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No friends yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Add friends to share POIs with them",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "Select a friend",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friends) { friend ->
                    FriendSelectionItem(
                        friend = friend,
                        isSelected = selectedFriendId == friend.otherUserId,
                        onClick = { selectedFriendId = friend.otherUserId }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(200) },
                label = { Text("Message (optional)") },
                placeholder = { Text("Add a personal note...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${message.length}/200") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedFriendId?.let { onShare(it, message.ifBlank { null }) }
                },
                enabled = selectedFriendId != null && !state.isUploading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.green)
            ) {
                if (state.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sharing...")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share POI")
                }
            }
        }
    }
}

@Composable
private fun FriendSelectionItem(
    friend: Friendship,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AppColors.green.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, AppColors.green) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (friend.otherUserAvatar != null) {
                AsyncImage(
                    model = friend.otherUserAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.otherUserName?.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = friend.otherUserName ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = AppColors.green
                )
            }
        }
    }
}

@Composable
private fun WizardNavigation(
    currentStep: CreatePoiStep,
    isStep1Valid: Boolean,
    isStep2Valid: Boolean,
    isUploading: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (currentStep) {
            CreatePoiStep.BASIC_INFO -> {
                TextButton(onClick = onCancel, enabled = !isUploading) {
                    Text("Cancel")
                }
                Button(
                    onClick = onNext,
                    enabled = isStep1Valid && !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.green)
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }

            CreatePoiStep.LOCATION_PHOTO -> {
                TextButton(onClick = onBack, enabled = !isUploading) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }
                Button(
                    onClick = onNext,
                    enabled = isStep2Valid && !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.green)
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }

            CreatePoiStep.SELECT_FRIEND -> {
                TextButton(onClick = onBack, enabled = !isUploading) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back")
                }
                Spacer(modifier = Modifier.width(1.dp))
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = AppColors.green,
                    shape = RoundedCornerShape(20.dp)
                ) else Modifier
            ),
        color = if (isSelected) AppColors.green.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) AppColors.green else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Platform-agnostic image preview from bytes.
 */
@Composable
expect fun ImagePreview(
    imageBytes: ByteArray,
    modifier: Modifier = Modifier
)
