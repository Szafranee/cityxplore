package app.cityxplore.profile.presentation

import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.profile.domain.ProfileRepository
import app.cityxplore.profile.domain.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileState {
    data object Loading : ProfileState
    data class Success(val profile: UserProfile) : ProfileState
    data class Error(val message: String) : ProfileState
}

class ProfileViewModel(
    private val repository: ProfileRepository
) : CityXploreBaseViewModel() {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state = _state.asStateFlow()

    init {
        fetchProfile()
    }

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
}
