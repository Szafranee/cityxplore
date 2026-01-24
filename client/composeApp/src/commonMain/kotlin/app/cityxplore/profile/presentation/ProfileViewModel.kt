package app.cityxplore.profile.presentation

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.achievements.domain.AchievementRepository
import app.cityxplore.core.cache.CacheKey
import app.cityxplore.core.cache.CacheManager
import app.cityxplore.core.cache.CacheState
import app.cityxplore.core.cityXploreDispatchers
import app.cityxplore.core.lifecycle.AppLifecycleObserver
import app.cityxplore.core.lifecycle.AppLifecycleState
import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.profile.data.UsernameAlreadyTakenException
import app.cityxplore.profile.domain.ProfileConstants
import app.cityxplore.profile.domain.ProfileRepository
import app.cityxplore.profile.domain.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sealed interface representing the state of the user profile screen.
 */
sealed interface ProfileState {
    /** Loading profile data from the backend */
    data object Loading : ProfileState

    /** Profile data successfully loaded */
    data class Success(
        val profile: UserProfile,
        val achievements: List<Achievement> = emptyList(),
        val isUpdating: Boolean = false,
        val updateError: String? = null
    ) : ProfileState

    /** An error occurred while loading profile data */
    data class Error(val message: String) : ProfileState
}

sealed interface ProfileEvent {
    data object ProfileUpdated : ProfileEvent
    data class EmailChangeInitiated(val newEmail: String) : ProfileEvent
}

/**
 * Internal UI state holder for combine
 */
private data class UiStateHolder(
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val updateError: String? = null,
    val error: String? = null
)

/**
 * Offline-first, lifecycle-aware ViewModel for the Profile screen.
 *
 * Key behaviors:
 * - **Reading:** Observes Flows from local Room database (single source of truth)
 * - **Refreshing:** Triggers network refresh, Room updates automatically
 * - **Lifecycle:** Doesn't reload on quick app switches, background refresh on long pause
 *
 * @property repository The profile repository for backend operations.
 * @property achievementRepository Repository for achievement data.
 * @property cacheManager Manager for tracking data freshness.
 * @property appLifecycleObserver Observer for app lifecycle events.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val achievementRepository: AchievementRepository,
    private val cacheManager: CacheManager,
    private val appLifecycleObserver: AppLifecycleObserver,
    private val supabase: SupabaseClient
) : CityXploreBaseViewModel() {

    private val _uiState = MutableStateFlow(UiStateHolder())

    /**
     * List of full URLs for predefined avatars.
     * Constructed dynamically using the configured Supabase Storage URL.
     */
    val predefinedAvatars: List<String> by lazy {
        ProfileConstants.AVATAR_FILENAMES.map {
            supabase.storage.from(ProfileConstants.AVATAR_BUCKET).publicUrl(it)
        }
    }

    /**
     * StateFlow emitting the current profile state.
     * Combines profile and achievements from Room with the UI state.
     */
    val state: StateFlow<ProfileState> = combine(
        repository.observeProfile(),
        achievementRepository.observeMyAchievements(),
        _uiState
    ) { profile: UserProfile?, achievements: List<Achievement>, uiState: UiStateHolder ->
        when {
            uiState.isLoading && profile == null -> ProfileState.Loading
            uiState.error != null && profile == null -> ProfileState.Error(uiState.error)
            profile != null -> ProfileState.Success(
                profile = profile,
                achievements = achievements.sortedWith(
                    compareByDescending<Achievement> { it.isUnlocked }
                        .thenBy { it.points }
                ),
                isUpdating = uiState.isUpdating,
                updateError = uiState.updateError
            )

            else -> ProfileState.Loading
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), ProfileState.Loading)

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeLifecycle()
        loadDataIfNeeded()
    }

    /**
     * Observes app lifecycle to handle resume events.
     * Prevents unnecessary data reloads on quick app switches.
     */
    private fun observeLifecycle() {
        scope.launch {
            appLifecycleObserver.lifecycleState.collect { state ->
                when (state) {
                    AppLifecycleState.RESUMED -> handleResume()
                    else -> { /* no action needed */
                    }
                }
            }
        }
    }

    /**
     * Handles app resume - decides whether to refresh data based on background duration.
     */
    private fun handleResume() {
        if (appLifecycleObserver.wasQuickSwitch()) {
            // Quick switch - don't reload anything
            return
        }

        if (appLifecycleObserver.shouldRefreshOnResume()) {
            // Long background - refresh data in the background
            refreshInBackground()
        }
    }

    /**
     * Loads data based on the cache state.
     */
    private fun loadDataIfNeeded() {
        val profileCacheState = cacheManager.getCacheState(CacheKey.PROFILE)
        val achievementsCacheState = cacheManager.getCacheState(CacheKey.ACHIEVEMENTS)

        when {
            profileCacheState == CacheState.EMPTY || achievementsCacheState == CacheState.EMPTY -> fetchProfile()
            profileCacheState == CacheState.EXPIRED || achievementsCacheState == CacheState.EXPIRED -> fetchProfile()
            profileCacheState == CacheState.STALE || achievementsCacheState == CacheState.STALE -> refreshInBackground()
            // FRESH - Room Flow will provide data
        }
    }

    /**
     * Fetches the current user's profile data from the backend.
     * Updates the state to [ProfileState.Success] on success, or [ProfileState.Error] on failure.
     */
    fun fetchProfile() {
        scope.launch(cityXploreDispatchers.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Refresh both profile and achievements
            val profileResult = repository.refreshProfile()
            val achievementsResult = achievementRepository.refreshMyAchievements()

            if (profileResult.isSuccess) {
                cacheManager.markAsFresh(CacheKey.PROFILE)
            }
            if (achievementsResult.isSuccess) {
                cacheManager.markAsFresh(CacheKey.ACHIEVEMENTS)
            }

            // If both failed, show an error (only if we have no cached data)
            if (profileResult.isFailure && achievementsResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = profileResult.exceptionOrNull()?.message ?: "Failed to load profile"
                )
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    /**
     * Refreshes data in the background without showing loading state.
     */
    private fun refreshInBackground() {
        scope.launch(cityXploreDispatchers.io) {
            repository.refreshProfile().onSuccess {
                cacheManager.markAsFresh(CacheKey.PROFILE)
            }
            achievementRepository.refreshMyAchievements().onSuccess {
                cacheManager.markAsFresh(CacheKey.ACHIEVEMENTS)
            }
            // Silently ignore errors during background refresh
        }
    }

    /**
     * Updates the user's profile with a new username and/or avatar.
     *
     * Validates that the username is not blank before attempting the update.
     * Handles [UsernameAlreadyTakenException] specially to provide clear feedback
     * to the user when they try to use a taken username.
     *
     * @param username The new username for the profile.
     * @param avatarUrl The new avatar URL, or null to keep the existing avatar.
     */
    fun updateProfile(username: String, avatarUrl: String?) {
        if (username.isBlank()) {
            _uiState.value = _uiState.value.copy(updateError = "Username cannot be empty")
            return
        }

        scope.launch(cityXploreDispatchers.io) {
            _uiState.value = _uiState.value.copy(isUpdating = true, updateError = null)

            repository.createProfile(username, avatarUrl)
                .onSuccess {
                    // Refresh to get updated data
                    repository.refreshProfile().onSuccess {
                        cacheManager.markAsFresh(CacheKey.PROFILE)
                    }
                    _uiState.value = _uiState.value.copy(isUpdating = false)
                    _events.send(ProfileEvent.ProfileUpdated)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateError = parseProfileError(error)
                    )
                }
        }
    }

    /**
     * Parses profile-related errors into user-friendly messages.
     *
     * @param error The exception thrown during profile operations.
     * @return A user-friendly error message suitable for display.
     */
    private fun parseProfileError(error: Throwable): String {
        return when (error) {
            is UsernameAlreadyTakenException -> error.message ?: "Username is already taken"
            else -> error.message ?: "Failed to update profile"
        }
    }

    /**
     * Deletes the user account.
     */
    fun deleteAccount(onSuccess: () -> Unit) {
        scope.launch(cityXploreDispatchers.io) {
            _uiState.value = _uiState.value.copy(isUpdating = true, updateError = null)

            repository.deleteAccount()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isUpdating = false)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateError = error.message ?: "Failed to delete account"
                    )
                }
        }
    }

    /**
     * Uploads and updates the user's avatar.
     *
     * @param imageBytes The image data.
     */
    fun updateAvatar(imageBytes: ByteArray) {
        val currentProfile = (state.value as? ProfileState.Success)?.profile ?: return

        scope.launch(cityXploreDispatchers.io) {
            _uiState.value = _uiState.value.copy(isUpdating = true, updateError = null)

            // 1. Upload avatar to storage
            val uploadResult = repository.uploadAvatar(imageBytes)

            if (uploadResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    updateError = uploadResult.exceptionOrNull()?.message ?: "Failed to upload avatar"
                )
                return@launch
            }

            val publicUrl = uploadResult.getOrThrow()

            // 2. Update profile with new URL
            repository.createProfile(currentProfile.username, publicUrl)
                .onSuccess {
                    repository.refreshProfile().onSuccess {
                        cacheManager.markAsFresh(CacheKey.PROFILE)
                    }
                    _uiState.value = _uiState.value.copy(isUpdating = false)
                    _events.send(ProfileEvent.ProfileUpdated)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateError = error.message ?: "Failed to update profile with new avatar"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(updateError = null)
    }

    /**
     * Updates the user's email address.
     *
     * @param newEmail The new email address.
     */
    fun updateEmail(newEmail: String) {
        val currentProfile = (state.value as? ProfileState.Success)?.profile ?: return

        if (newEmail.isBlank() || newEmail == currentProfile.email) return

        scope.launch(cityXploreDispatchers.io) {
            _uiState.value = _uiState.value.copy(isUpdating = true, updateError = null)

            repository.updateEmail(newEmail)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isUpdating = false)
                    _events.send(ProfileEvent.EmailChangeInitiated(newEmail))
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateError = e.message ?: "Failed to update email"
                    )
                }
        }
    }
}
