package app.cityxplore.profile.presentation

import app.cityxplore.achievements.domain.Achievement
import app.cityxplore.achievements.domain.AchievementRepository
import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.profile.data.UsernameAlreadyTakenException
import app.cityxplore.profile.domain.ProfileRepository
import app.cityxplore.profile.domain.UserProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
 * ViewModel managing user profile state and operations.
 *
 * This ViewModel handles fetching and displaying user profile data including
 * username, avatar, total distance travelled, and total POIs discovered.
 *
 * @property repository The profile repository for backend operations.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val achievementRepository: AchievementRepository
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)

    /**
     * StateFlow emitting the current profile state.
     * UI components observe this to display profile data or loading/error states.
     */
    val state = _state.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        fetchProfile()
    }

    /**
     * Fetches the current user's profile data from the backend.
     * Updates the state to [ProfileState.Success] on success, or [ProfileState.Error] on failure.
     */
    fun fetchProfile() {
        scope.launch {
            // Only set loading if we don't have data, or if we want to refresh fully
            if (_state.value !is ProfileState.Success) {
                _state.value = ProfileState.Loading
            }

            fetchAndMergeData()
                .onSuccess { (profile, achievements) ->
                    _state.value = ProfileState.Success(
                        profile = profile,
                        achievements = achievements
                    )
                }
                .onFailure { error ->
                    _state.value =
                        ProfileState.Error(error.message ?: "Failed to load profile")
                }
        }
    }

    /**
     * Updates the user's profile with new username and/or avatar.
     *
     * Validates that the username is not blank before attempting the update.
     * Handles [UsernameAlreadyTakenException] specially to provide clear feedback
     * to the user when they try to use a taken username.
     *
     * @param username The new username for the profile.
     * @param avatarUrl The new avatar URL, or null to keep the existing avatar.
     */
    fun updateProfile(username: String, avatarUrl: String?) {
        val currentState = _state.value
        if (currentState !is ProfileState.Success) return

        if (username.isBlank()) {
            _state.value = currentState.copy(updateError = "Username cannot be empty")
            return
        }

        scope.launch {
            _state.value = currentState.copy(isUpdating = true, updateError = null)

            repository.createProfile(username, avatarUrl)
                .onSuccess {
                    fetchAndMergeData()
                        .onSuccess { (profile, achievements) ->
                            _state.value = ProfileState.Success(
                                profile = profile,
                                achievements = achievements
                            )
                            _events.send(ProfileEvent.ProfileUpdated)
                        }
                        .onFailure {
                            _state.value = currentState.copy(
                                isUpdating = false,
                                updateError = "Profile updated but failed to refresh"
                            )
                        }
                }
                .onFailure { error ->
                    val errorMessage = parseProfileError(error)
                    _state.value = currentState.copy(
                        isUpdating = false,
                        updateError = errorMessage
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
        val currentState = _state.value
        if (currentState !is ProfileState.Success) return

        scope.launch {
            _state.value = currentState.copy(isUpdating = true, updateError = null)

            repository.deleteAccount()
                .onSuccess {
                    _state.value = currentState.copy(isUpdating = false)
                    onSuccess()
                }
                .onFailure { error ->
                    _state.value = currentState.copy(
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
        val currentState = _state.value
        if (currentState !is ProfileState.Success) return

        scope.launch {
            _state.value = currentState.copy(isUpdating = true, updateError = null)

            // 1. Upload avatar to storage
            val uploadResult = repository.uploadAvatar(imageBytes)

            if (uploadResult.isFailure) {
                _state.value = currentState.copy(
                    isUpdating = false,
                    updateError = uploadResult.exceptionOrNull()?.message ?: "Failed to upload avatar"
                )
                return@launch
            }

            val publicUrl = uploadResult.getOrThrow()

            // 2. Update profile with new URL
            // Reuse existing update logic but with the new URL
            repository.createProfile(currentState.profile.username, publicUrl)
                .onSuccess {
                    fetchAndMergeData()
                        .onSuccess { (profile, achievements) ->
                            _state.value = ProfileState.Success(
                                profile = profile,
                                achievements = achievements
                            )
                            _events.send(ProfileEvent.ProfileUpdated)
                        }
                        .onFailure {
                            _state.value = currentState.copy(
                                isUpdating = false,
                                updateError = "Avatar uploaded but failed to refresh profile"
                            )
                        }
                }
                .onFailure { error ->
                    _state.value = currentState.copy(
                        isUpdating = false,
                        updateError = error.message ?: "Failed to update profile with new avatar"
                    )
                }
        }
    }

    private suspend fun fetchAndMergeData(): Result<Pair<UserProfile, List<Achievement>>> {
        val profileResult = repository.getProfile()
        if (profileResult.isFailure) {
            return Result.failure(
                profileResult.exceptionOrNull() ?: Exception("Failed to load profile")
            )
        }
        val profile = profileResult.getOrThrow()

        // Fetch both all achievements and user's unlocked achievements
        val allAchievementsResult = achievementRepository.getAllAchievements()
        val myAchievementsResult = achievementRepository.getMyAchievements()

        val allAchievements = allAchievementsResult.getOrDefault(emptyList())
        val myAchievements = myAchievementsResult.getOrDefault(emptyList())

        // Merge lists: Update the "isUnlocked" status for achievements that the user has
        val mergedAchievements = if (allAchievements.isNotEmpty()) {
            val unlockedIds = myAchievements.map { it.id }.toSet()
            allAchievements.map { achievement ->
                if (achievement.id in unlockedIds) {
                    // Use the unlocked version to keep unlockedAt date if needed,
                    // or just set isUnlocked = true
                    achievement.copy(isUnlocked = true)
                } else {
                    achievement
                }
            }
        } else {
            // Fallback to myAchievements if allAchievements call failed?
            // Or a simple empty list if both failed.
            myAchievements
        }

        // Sort: Unlocked first, then by points/name
        val sortedAchievements = mergedAchievements.sortedWith(
            compareByDescending<Achievement> { it.isUnlocked }
                .thenBy { it.points }
        )

        return Result.success(profile to sortedAchievements)
    }

    fun clearError() {
        _state.update { currentState ->
            if (currentState is ProfileState.Success) {
                currentState.copy(updateError = null)
            } else {
                currentState
            }
        }
    }

    /**
     * Updates the user's email address.
     *
     * @param newEmail The new email address.
     */
    fun updateEmail(newEmail: String) {
        val currentState = state.value
        if (currentState !is ProfileState.Success) return

        if (newEmail.isBlank() || newEmail == currentState.profile.email) return

        _state.update { currentState.copy(isUpdating = true, updateError = null) }

        scope.launch {
            repository.updateEmail(newEmail)
                .onSuccess {
                    _state.update { currentState.copy(isUpdating = false) }
                    _events.send(ProfileEvent.EmailChangeInitiated(newEmail))
                }
                .onFailure { e ->
                    _state.update {
                        currentState.copy(
                            isUpdating = false,
                            updateError = e.message ?: "Failed to update email"
                        )
                    }
                }
        }
    }
}
