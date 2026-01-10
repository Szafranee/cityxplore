package app.cityxplore.profile.presentation

import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.profile.domain.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
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

    private val _initialUsername = MutableStateFlow<String?>(null)

    /**
     * StateFlow emitting a suggested username derived from user metadata.
     * This is typically populated from social provider data (e.g. Google full name).
     */
    val initialUsername: StateFlow<String?> = _initialUsername.asStateFlow()

    private val _uploadedAvatarUrl = MutableStateFlow<String?>(null)

    /**
     * StateFlow emitting the URL of the uploaded avatar image.
     * This is updated after a successful avatar upload.
     */
    val uploadedAvatarUrl = _uploadedAvatarUrl.asStateFlow()

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
     * Creates a user profile with the specified username and optional avatar URL.
     *
     * @param username The desired username for the profile.
     * @param avatarUrl The optional URL to the user's avatar image.
     */
    fun createProfile(username: String, avatarUrl: String?) {
        scope.launch {
            _state.value = OnboardingState.Loading
            repository.createProfile(username, avatarUrl)
                .onSuccess {
                    _state.value = OnboardingState.Success
                }
                .onFailure {
                    val message = if (it is ClientRequestException && it.response.status == HttpStatusCode.Conflict) {
                        "Username or email already taken."
                    } else {
                        it.message ?: "Failed to create profile"
                    }
                    _state.value = OnboardingState.Error(message)
                }
        }
    }

    /**
     * Uploads an avatar image for the user.
     *
     * @param bytes The byte array of the avatar image to be uploaded.
     */
    fun uploadAvatar(bytes: ByteArray) {
        scope.launch {
            _state.value = OnboardingState.Loading
            repository.uploadAvatar(bytes)
                .onSuccess { url ->
                    _uploadedAvatarUrl.value = url
                    _state.value = OnboardingState.Idle
                }
                .onFailure {
                    _state.value = OnboardingState.Error("Failed to upload avatar: ${it.message}")
                }
        }
    }
}
