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

sealed interface OnboardingState {
    data object Idle : OnboardingState
    data object Loading : OnboardingState
    data object Success : OnboardingState
    data class Error(val message: String) : OnboardingState
}

class OnboardingViewModel(
    private val repository: ProfileRepository,
    private val supabase: SupabaseClient
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val state = _state.asStateFlow()

    private val _initialUsername = MutableStateFlow<String?>(null)
    val initialUsername: StateFlow<String?> = _initialUsername.asStateFlow()

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
}
