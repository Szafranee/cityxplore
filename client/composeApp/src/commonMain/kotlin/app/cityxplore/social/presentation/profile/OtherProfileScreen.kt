package app.cityxplore.social.presentation.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.achievements.domain.AchievementRepository
import app.cityxplore.social.domain.model.RankingEntry
import app.cityxplore.social.domain.repository.SocialRepository
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Sealed interface representing the state of another user's profile screen.
 */
sealed interface OtherProfileState {
    data object Loading : OtherProfileState
    data class Success(
        val profile: RankingEntry,
        val achievements: List<Achievement> = emptyList()
    ) : OtherProfileState

    data object Blocked : OtherProfileState  // User is blocked by profile owner
    data class Error(val message: String) : OtherProfileState
}

/**
 * ViewModel for OtherProfileScreen - loads another user's profile data.
 *
 * Fetches user profile information from the social repository and their achievements
 * from the achievement repository. Manages loading states and error handling.
 *
 * @param socialRepository Repository for fetching user profile data.
 * @param achievementRepository Repository for fetching user achievements.
 */
class OtherProfileViewModel(
    private val socialRepository: SocialRepository,
    private val achievementRepository: AchievementRepository
) : ViewModel() {
    private val _state = MutableStateFlow<OtherProfileState>(OtherProfileState.Loading)
    val state = _state.asStateFlow()

    /**
     * Loads profile data and achievements for the specified user.
     *
     * Fetches profile information (stats, avatar, username) and the user's achievements.
     * Updates the state to Loading, Success, or Error based on the result.
     *
     * @param userId The unique identifier of the user whose profile to load.
     */
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _state.value = OtherProfileState.Loading

            // First check if we're blocked by this user
            socialRepository.checkIfBlocked(userId)
                .onSuccess { isBlocked ->
                    if (isBlocked) {
                        _state.value = OtherProfileState.Blocked
                        return@launch
                    }

                    // Not blocked, proceed to load profile
                    socialRepository.getFriendProfile(userId)
                        .onSuccess { profile ->
                            // Fetch achievements for this user (now supported by backend)
                            val achievements = achievementRepository.getUserAchievements(userId)
                                .getOrElse { emptyList() }
                            _state.value = OtherProfileState.Success(profile, achievements)
                        }
                        .onFailure { error ->
                            _state.value = OtherProfileState.Error(error.message ?: "Failed to load profile")
                        }
                }
                .onFailure { error ->
                    // If we can't check block status, still try to load profile
                    socialRepository.getFriendProfile(userId)
                        .onSuccess { profile ->
                            val achievements = achievementRepository.getUserAchievements(userId)
                                .getOrElse { emptyList() }
                            _state.value = OtherProfileState.Success(profile, achievements)
                        }
                        .onFailure {
                            _state.value = OtherProfileState.Error(error.message ?: "Failed to load profile")
                        }
                }
        }
    }
}

/**
 * Screen displaying another user's profile information, matching the layout of ProfileScreen.
 * Does NOT include: settings, avatar editing, sign-out button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    userId: String,
    onBack: () -> Unit
) {
    val socialRepository: SocialRepository = koinInject()
    val achievementRepository: AchievementRepository = koinInject()
    val viewModel = remember { OtherProfileViewModel(socialRepository, achievementRepository) }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val s = state) {
                            is OtherProfileState.Success -> s.profile.username
                            else -> "Profile"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is OtherProfileState.Loading -> CircularProgressIndicator()
                is OtherProfileState.Blocked -> BlockedContent(onBack)
                is OtherProfileState.Error -> ErrorContent(
                    message = currentState.message,
                    onRetry = { viewModel.loadProfile(userId) }
                )

                is OtherProfileState.Success -> {
                    OtherProfileContent(
                        profile = currentState.profile,
                        achievements = currentState.achievements
                    )
                }
            }
        }
    }
}

/**
 * Content shown when user is blocked by the profile owner.
 */
@Composable
private fun BlockedContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Profile Blocked",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This user has blocked you. You cannot view their profile.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onBack) {
            Text("Go Back")
        }
    }
}

/**
 * Profile content layout matching ProfileScreen structure.
 * Displays: avatar, username, level, XP bar, stats, and achievements.
 */
@Composable
private fun OtherProfileContent(
    profile: RankingEntry,
    achievements: List<Achievement>
) {
    val scrollState = rememberScrollState()

    // Calculate level from achievement points (same logic as UserProfile)
    val level = calculateLevel(profile.totalAchievementPoints)
    val levelProgress = calculateLevelProgress(profile.totalAchievementPoints)
    val xpInCurrentLevel = calculateXpInCurrentLevel(profile.totalAchievementPoints)
    val xpNeededForNextLevel = calculateXpNeededForNextLevel(profile.totalAchievementPoints)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Section (no edit button)
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Username & Level
        Text(
            text = profile.username.ifBlank { "Unknown User" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Level $level Explorer",
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
                progress = { levelProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$xpInCurrentLevel / $xpNeededForNextLevel XP",
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
                value = profile.totalAchievementPoints.toString(),
                modifier = Modifier.weight(1f)
            )
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
                text = "No achievements to display.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        } else {
            AchievementsGrid(achievements)
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
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
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
    onRetry: () -> Unit
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
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun AchievementsGrid(achievements: List<Achievement>) {
    var expanded by remember { mutableStateOf(false) }
    val displayAchievements = if (expanded) achievements else achievements.take(6)

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
                        modifier = Modifier.weight(1f)
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
                    imageVector = if (achievement.isUnlocked) Icons.Rounded.Star else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = achievement.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Level calculation helpers (same as UserProfile)
/**
 * Calculates the user's level based on total achievement points.
 * Uses the same formula as UserProfile: Level 0->1 needs 100 pts, every subsequent level needs +25 pts more.
 */
private fun calculateLevel(points: Int): Int {
    var level = 0
    var cost = 100
    var remaining = points

    while (remaining >= cost) {
        remaining -= cost
        level++
        cost += 25
    }
    return level
}

/**
 * Calculates the progress percentage within the current level (0.0 to 1.0).
 */
private fun calculateLevelProgress(points: Int): Float {
    var level = 0
    var cost = 100
    var remaining = points

    while (remaining >= cost) {
        remaining -= cost
        level++
        cost += 25
    }

    // remaining = XP at the current level, cost = XP needed for the next level
    return if (cost > 0) remaining.toFloat() / cost.toFloat() else 0f
}

/**
 * Calculates XP accumulated at the current level.
 */
private fun calculateXpInCurrentLevel(points: Int): Int {
    var cost = 100
    var remaining = points

    while (remaining >= cost) {
        remaining -= cost
        cost += 25
    }
    return remaining
}

/**
 * Calculates XP needed for the next level.
 */
private fun calculateXpNeededForNextLevel(points: Int): Int {
    var cost = 100
    var remaining = points

    while (remaining >= cost) {
        remaining -= cost
        cost += 25
    }
    return cost
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        "${(meters / 1000).toString().take(4)} km"
    } else {
        "${meters.toInt()} m"
    }
}
