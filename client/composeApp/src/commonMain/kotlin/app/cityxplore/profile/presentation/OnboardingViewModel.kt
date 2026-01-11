package app.cityxplore.profile.presentation

import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.profile.data.UsernameAlreadyTakenException
import app.cityxplore.profile.domain.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed interface representing the state of the onboarding/profile creation process.
 */
sealed interface OnboardingState {
    /** Initial idle state before any action is taken */
    data object Idle : OnboardingState

    /** Profile creation is in progress */
    data object Loading : OnboardingState

    /** Profile successfully created */
    data object Success : OnboardingState

    /** An error occurred during profile creation */
    data class Error(val message: String) : OnboardingState
}

/**
 * Sealed interface representing the state of avatar upload.
 */
sealed interface AvatarUploadState {
    data object Idle : AvatarUploadState
    data object Loading : AvatarUploadState
    data object Success : AvatarUploadState
    data class Error(val message: String) : AvatarUploadState
}

/**
 * ViewModel managing user onboarding and profile creation.
 *
 * This ViewModel handles the initial profile setup for new users, including
 * fetching user metadata from social providers (e.g. full name, preferred username)
 * and creating the user profile in the backend system.
 *
 * @property repository The profile repository for backend operations.
 * @property supabase The Supabase client for retrieving user metadata.
 */
class OnboardingViewModel(
    private val repository: ProfileRepository,
    private val supabase: SupabaseClient
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Idle)

    /**
     * StateFlow emitting the current onboarding state.
     */
    val state = _state.asStateFlow()

    private val _uploadState = MutableStateFlow<AvatarUploadState>(AvatarUploadState.Idle)

    /**
     * StateFlow emitting the current avatar upload state.
     */
    val uploadState = _uploadState.asStateFlow()

    private val _initialUsername = MutableStateFlow<String?>(null)

    /**
     * StateFlow emitting a suggested username derived from user metadata.
     * This is typically populated from social provider data (e.g. Google full name).
     */
    val initialUsername: StateFlow<String?> = _initialUsername.asStateFlow()

    /** Pending avatar bytes to be uploaded after profile creation. */
    private var pendingAvatarBytes: ByteArray? = null

    /**
     * Fetches user metadata from the Supabase authentication session.
     * Extracts the preferred username, full name, or name to suggest as an initial username.
     */
    fun fetchUserMetadata() {
        scope.launch {
            val user = supabase.auth.currentUserOrNull()
            val metadata = user?.userMetadata
            val fullName = metadata?.get("full_name")?.toString()?.replace("\"", "")
            val name = metadata?.get("name")?.toString()?.replace("\"", "")
            val preferredUsername = metadata?.get("preferred_username")?.toString()?.replace("\"", "")

            _initialUsername.value = preferredUsername ?: fullName ?: name
        }
    }

    /**
     * Stores the avatar bytes to be uploaded during profile creation.
     */
    fun setAvatar(bytes: ByteArray?) {
        pendingAvatarBytes = bytes
        // Pending bytes will be uploaded after profile creation
    }

    /**
     * Creates a user profile with the specified username.
     * If a custom avatar was selected, it handles the creation -> upload -> update flow.
     *
     * @param username The desired username for the profile.
     * @param selectedAvatarUrl URL of a predefined avatar (if no custom one selected) or null.
     */
    fun createProfile(username: String, selectedAvatarUrl: String?) {
        scope.launch {
            _state.value = OnboardingState.Loading

            // 1. Create Profile first
            // If we have pending bytes, we use null for avatar initially.
            // If we don't have pending bytes, we use the selected predefined URL (or null).
            val initialAvatarUrl = if (pendingAvatarBytes != null) null else selectedAvatarUrl

            val createResult = repository.createProfile(username, initialAvatarUrl)

            createResult.onFailure {
                handleError(it)
                return@launch
            }

            // 2. If we have a custom avatar to upload, do it now (User exists)
            if (pendingAvatarBytes != null) {
                _uploadState.value = AvatarUploadState.Loading
                val uploadResult = repository.uploadAvatar(pendingAvatarBytes!!)

                uploadResult.onFailure {
                    // Profile created but avatar failed.
                    _uploadState.value = AvatarUploadState.Error("Profile created, but avatar upload failed.")
                    // Even if avatar fails, the profile is created, so we can consider onboarding success 
                    // or ask user to retry upload (but we don't have retry logic for just upload here).
                    // For now, proceed.
                    _state.value = OnboardingState.Success
                    return@launch
                }

                val uploadedUrl = uploadResult.getOrThrow()

                // 3. Update Profile with new Avatar URL
                val updateResult = repository.createProfile(username, uploadedUrl)
                if (updateResult.isFailure) {
                    _uploadState.value = AvatarUploadState.Error("Avatar uploaded but profile update failed.")
                } else {
                    _uploadState.value = AvatarUploadState.Success
                }
            }

            _state.value = OnboardingState.Success
        }
    }

    /**
     * Handles errors that occur during profile creation.
     *
     * Provides user-friendly messages for common error cases such as
     * username conflicts.
     *
     * @param error The exception that occurred during profile creation.
     */
    private fun handleError(error: Throwable) {
        val message = when (error) {
            is UsernameAlreadyTakenException -> error.message ?: "Username is already taken"
            else -> error.message ?: "Failed to create profile"
        }
        _state.value = OnboardingState.Error(message)
    }
}
