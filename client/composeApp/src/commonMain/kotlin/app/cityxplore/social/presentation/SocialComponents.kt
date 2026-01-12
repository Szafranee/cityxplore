package app.cityxplore.social.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.model.FriendshipStatus
import app.cityxplore.social.domain.model.RankingEntry
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * Displays the content of the Rankings tab, with a sub-tab switcher for Global vs Friends rankings.
 *
 * @param global List of ranking entries for the global leaderboard.
 * @param friends List of ranking entries for the user's friends leaderboard.
 * @param initialSubTab Initial sub-tab to display (0 = Global, 1 = Friends)
 * @param onUserSelected Callback with userId and isGlobalRanking (true = Global, false = Friends)
 */
@Composable
fun RankingListContent(
    global: List<RankingEntry>,
    friends: List<RankingEntry>,
    initialSubTab: Int = 0,
    onUserSelected: (String, isGlobalRanking: Boolean) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(initialSubTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedSubTab) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("Global") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Friends") })
        }

        val usersList = if (selectedSubTab == 0) global else friends

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(usersList) { entry ->
                RankingItem(entry) { userId ->
                    onUserSelected(userId, selectedSubTab == 0)
                }
            }
            if (usersList.isEmpty()) {
                item {
                    Text(
                        "No rankings available.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/**
 * Displays a single row ITEM in the ranking list.
 *
 * @param entry The ranking entry to display.
 * @param onUserSelected Callback when user is clicked, receives userId
 */
@Composable
fun RankingItem(entry: RankingEntry, onUserSelected: (String) -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.clickable { onUserSelected(entry.userId) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )

            AsyncImage(
                model = entry.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop,
                // Fallback icon if the avatar URL is invalid or fails to load
                error = rememberVectorPainter(Icons.Default.Person)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(entry.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Points: ${entry.totalAchievementPoints}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "POIs: ${entry.totalPoisDiscovered}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Displays the content of the Friends tab, including pending requests and the active friends list.
 *
 * @param friends List of active friendships.
 * @param pendingRequests List of pending incoming friendship requests.
 * @param onAccept Callback when a user accepts a request.
 * @param onDecline Callback when a user declines a request.
 */
@Composable
fun FriendsListContent(
    friends: List<Friendship>,
    pendingRequests: List<Friendship>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onUserSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (pendingRequests.isNotEmpty()) {
            item {
                Text(
                    "Pending Requests",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(pendingRequests) { request ->
                FriendRequestItem(request, onAccept, onDecline)
            }
            item {
                HorizontalDivider()
            }
        }

        item {
            Text("My Friends", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }

        if (friends.isEmpty()) {
            item {
                Text("You have no friends yet. Add some!", modifier = Modifier.padding(8.dp))
            }
        } else {
            items(friends) { friend ->
                FriendItem(
                    friendship = friend,
                    onDelete = onDelete,
                    onBlock = onBlock,
                    onUnblock = onUnblock,
                    onClick = { onUserSelected(friend.otherUserId()) }
                )
            }
        }
    }
}

private fun Friendship.otherUserId(): String =
    otherUserId ?: requesterId

@Composable
fun FriendRequestItem(
    friendship: Friendship,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // We prefer displaying the resolved name (from profile enricher), fallback to generic text if missing
                Text(
                    friendship.otherUserName ?: "User",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("Sent you a friend request", style = MaterialTheme.typography.bodySmall)
            }

            Row {
                IconButton(onClick = { onAccept(friendship.id) }) {
                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.Green)
                }
                IconButton(onClick = { onDecline(friendship.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Decline", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    friendship: Friendship,
    onDelete: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = friendship.otherUserAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.Person)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(friendship.otherUserName ?: "User", style = MaterialTheme.typography.bodyLarge)
                    if (friendship.status == FriendshipStatus.BLOCKED) {
                        Text(
                            "Blocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }

                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("View profile") },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove friend") },
                        onClick = {
                            menuExpanded = false
                            confirmAction = { onDelete(friendship.id) }
                        }
                    )
                    if (friendship.status == FriendshipStatus.BLOCKED) {
                        DropdownMenuItem(
                            text = { Text("Unblock") },
                            onClick = {
                                menuExpanded = false
                                confirmAction = { onUnblock(friendship.id) }
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Block") },
                            onClick = {
                                menuExpanded = false
                                confirmAction = { onBlock(friendship.id) }
                            }
                        )
                    }
                }
            }
        }
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Are you sure?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmAction = null
                    scope.launch { action() }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onSendInvite: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend") },
        text = {
            Column {
                Text("Enter username to invite:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    label = { Text("Username") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendInvite(username) },
                enabled = username.isNotBlank()
            ) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Wrapper for vector painter to use in common code where resource accessors might differ slightly
// or to avoid verbose checks in UI code.
@Composable
fun rememberVectorPainter(image: androidx.compose.ui.graphics.vector.ImageVector) =
    androidx.compose.ui.graphics.vector.rememberVectorPainter(image)
