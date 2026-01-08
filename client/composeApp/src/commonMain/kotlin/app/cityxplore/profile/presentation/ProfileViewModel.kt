package app.cityxplore.profile.presentation

import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.profile.domain.ProfileRepository
import app.cityxplore.profile.domain.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed interface representing the state of the user profile screen.
 */
sealed interface ProfileState {
    /** Loading profile data from the backend */
    data object Loading : ProfileState

    /** Profile data successfully loaded */
    data class Success(val profile: UserProfile) : ProfileState

    /** An error occurred while loading profile data */
    data class Error(val message: String) : ProfileState
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
    private val repository: ProfileRepository
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)

    /**
     * StateFlow emitting the current profile state.
     * UI components observe this to display profile data or loading/error states.
     */
    val state = _state.asStateFlow()

    init {
        fetchProfile()
    }

    /**
     * Fetches the current user's profile data from the backend.
     * Updates the state to [ProfileState.Success] on success, or [ProfileState.Error] on failure.
     */
    fun fetchProfile() {
        scope.launch {
            _state.value = ProfileState.Loading
            repository.getProfile()
                .onSuccess {
                    _state.value = ProfileState.Success(it)
                }
                .onFailure {
                    _state.value = ProfileState.Error(it.message ?: "Failed to load profile")
                }
        }
    }

    /**
     * Updates the user's profile with new data.
     */
    fun updateProfile(username: String, avatarUrl: String?) {
        scope.launch {
            // Keep the current profile visible while updating, or show a loading indicator?
            // For now, simple approach:
            val currentProfile = (_state.value as? ProfileState.Success)?.profile
            _state.value = ProfileState.Loading

            repository.createProfile(username, avatarUrl)
                .onSuccess {
                    // Refresh profile to get updated data
                    fetchProfile()
                }
                .onFailure { error ->
                    // Restore previous profile if possible, otherwise show error
                    if (currentProfile != null) {
                        // Ideally we should show a snackbar error, but for now we fallback to Error state or keep old one.
                        // Let's just go to Error state to be safe and rigorous.
                        _state.value = ProfileState.Error(error.message ?: "Failed to update profile")
                    } else {
                        _state.value = ProfileState.Error(error.message ?: "Failed to update profile")
                    }
                }
        }
    }
}
