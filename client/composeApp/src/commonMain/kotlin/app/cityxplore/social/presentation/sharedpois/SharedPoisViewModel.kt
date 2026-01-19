package app.cityxplore.social.presentation.sharedpois

import app.cityxplore.core.connectivity.ConnectivityObserver
import app.cityxplore.core.lifecycle.AppLifecycleObserver
import app.cityxplore.core.lifecycle.AppLifecycleState
import app.cityxplore.platform.CityXploreBaseViewModel
import app.cityxplore.social.domain.DeleteSharedPoiUseCase
import app.cityxplore.social.domain.GetReceivedSharedPoisUseCase
import app.cityxplore.social.domain.GetSentSharedPoisUseCase
import app.cityxplore.social.domain.GetUnviewedSharedPoisUseCase
import app.cityxplore.social.domain.MarkSharedPoiViewedUseCase
import app.cityxplore.social.domain.SharePoiUseCase
import app.cityxplore.social.domain.model.SharePoiRequest
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.domain.repository.SharedPoiRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Shared POIs UI state and user interactions.
 * Supports lifecycle-aware refreshing to avoid unnecessary reloads on quick app switches.
 *
 * **Note:** Sharing POIs requires network connectivity and does not work offline.
 */
class SharedPoisViewModel(
    private val getSentSharedPoisUseCase: GetSentSharedPoisUseCase,
    private val getReceivedSharedPoisUseCase: GetReceivedSharedPoisUseCase,
    private val getUnviewedSharedPoisUseCase: GetUnviewedSharedPoisUseCase,
    private val sharePoiUseCase: SharePoiUseCase,
    private val markSharedPoiViewedUseCase: MarkSharedPoiViewedUseCase,
    private val deleteSharedPoiUseCase: DeleteSharedPoiUseCase,
    private val sharedPoiRepository: SharedPoiRepository,
    private val appLifecycleObserver: AppLifecycleObserver,
    private val connectivityObserver: ConnectivityObserver
) : CityXploreBaseViewModel() {

    private val offlineMessage = "This feature requires an internet connection"

    // Internal state for loading/error
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SharedPoisUiState> = combine(
        getReceivedSharedPoisUseCase(),
        getSentSharedPoisUseCase(),
        getUnviewedSharedPoisUseCase.count(),
        _isLoading,
        _error
    ) { received, sent, unviewedCount, isLoading, error ->
        when {
            error != null -> SharedPoisUiState.Error(error)
            isLoading -> SharedPoisUiState.Loading
            else -> SharedPoisUiState.Content(
                receivedPois = received,
                sentPois = sent,
                unviewedCount = unviewedCount
            )
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), SharedPoisUiState.Loading)

    private val _uiEvents = MutableSharedFlow<SharedPoisUiEvent>()
    val uiEvents: SharedFlow<SharedPoisUiEvent> = _uiEvents.asSharedFlow()

    private val _createPoiState = MutableStateFlow(CreateCustomPoiState())
    val createPoiState: StateFlow<CreateCustomPoiState> = _createPoiState.asStateFlow()

    init {
        observeLifecycle()
        refresh()
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
     * Refreshes data in the background without showing loading state.
     */
    private fun refreshInBackground() {
        scope.launch {
            getReceivedSharedPoisUseCase.refresh()
            getSentSharedPoisUseCase.refresh()
            getUnviewedSharedPoisUseCase.refresh()
            // Silently ignore errors during background refresh
        }
    }

    fun refresh() {
        scope.launch {
            _isLoading.value = true
            _error.value = null

            val results = listOf(
                getReceivedSharedPoisUseCase.refresh(),
                getSentSharedPoisUseCase.refresh(),
                getUnviewedSharedPoisUseCase.refresh()
            )

            val firstFailure = results.firstOrNull { it.isFailure }
            if (firstFailure != null) {
                _error.value = firstFailure.exceptionOrNull()?.message ?: "Failed to load shared POIs"
            }

            _isLoading.value = false
        }
    }

    // Create Custom POI Dialog

    fun updateCreatePoiName(name: String) {
        _createPoiState.update { it.copy(name = name.take(200)) }
    }

    fun updateCreatePoiDescription(description: String) {
        _createPoiState.update { it.copy(description = description.take(1000)) }
    }

    fun updateCreatePoiCategory(category: String) {
        _createPoiState.update { it.copy(category = category) }
    }

    fun updateCreatePoiLocation(latitude: Double, longitude: Double) {
        _createPoiState.update { it.copy(latitude = latitude, longitude = longitude) }
    }

    fun updateCreatePoiImageBytes(bytes: ByteArray?) {
        _createPoiState.update { it.copy(imageBytes = bytes) }
    }

    fun showLocationPicker() {
        _createPoiState.update { it.copy(isLocationPickerVisible = true) }
    }

    fun hideLocationPicker() {
        _createPoiState.update { it.copy(isLocationPickerVisible = false) }
    }

    fun resetCreatePoiState() {
        _createPoiState.value = CreateCustomPoiState()
    }

    /** Navigate to the next step in the wizard */
    fun nextStep() {
        _createPoiState.update { state ->
            when (state.currentStep) {
                CreatePoiStep.BASIC_INFO -> {
                    if (state.isStep1Valid) state.copy(currentStep = CreatePoiStep.LOCATION_PHOTO)
                    else state
                }

                CreatePoiStep.LOCATION_PHOTO -> {
                    if (state.isStep2Valid) state.copy(currentStep = CreatePoiStep.SELECT_FRIEND)
                    else state
                }

                CreatePoiStep.SELECT_FRIEND -> state // Already at the last step
            }
        }
    }

    /** Navigate to the previous step in the wizard */
    fun previousStep() {
        _createPoiState.update { state ->
            when (state.currentStep) {
                CreatePoiStep.BASIC_INFO -> state // Already at the first step
                CreatePoiStep.LOCATION_PHOTO -> state.copy(currentStep = CreatePoiStep.BASIC_INFO)
                CreatePoiStep.SELECT_FRIEND -> state.copy(currentStep = CreatePoiStep.LOCATION_PHOTO)
            }
        }
    }

    fun hideShareDialog() {
        resetCreatePoiState()
    }

    fun shareCustomPoi(recipientId: String, message: String?) {
        val state = _createPoiState.value
        if (!state.isValid) return

        scope.launch {
            // Check connectivity first
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.emit(SharedPoisUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            // Mark as uploading
            _createPoiState.update { it.copy(isUploading = true) }

            try {
                // Upload image if present
                var imageUrl: String? = null
                if (state.imageBytes != null) {
                    val uploadResult = sharedPoiRepository.uploadPoiImage(state.imageBytes)
                    if (uploadResult.isFailure) {
                        _uiEvents.emit(
                            SharedPoisUiEvent.ShowMessage(
                                "Failed to upload image: ${uploadResult.exceptionOrNull()?.message}"
                            )
                        )
                        _createPoiState.update { it.copy(isUploading = false) }
                        return@launch
                    }
                    imageUrl = uploadResult.getOrNull()
                }

                // Create CustomPoi with uploaded image URL
                val customPoi = state.toCustomPoi(imageUrl)
                if (customPoi == null) {
                    _createPoiState.update {
                        it.copy(
                            isUploading = false,
                            createError = "Failed to create POI: Invalid data. Please check all fields."
                        )
                    }
                    _uiEvents.emit(SharedPoisUiEvent.ShowMessage("Failed to create POI: Invalid data"))
                    return@launch
                }

                val request = SharePoiRequest(
                    recipientId = recipientId,
                    customPoi = customPoi,
                    message = message?.trim()?.ifEmpty { null }
                )

                sharePoiUseCase(request)
                    .onSuccess {
                        // First, emit ShareSuccess to close the dialogue
                        _uiEvents.emit(SharedPoisUiEvent.ShareSuccess)
                        // Then show the success message (after the dialogue is closed)
                        _uiEvents.emit(SharedPoisUiEvent.ShowMessage("POI shared successfully!"))
                        resetCreatePoiState()
                        refresh()
                    }
                    .onFailure { error ->
                        _createPoiState.update { it.copy(isUploading = false) }
                        _uiEvents.emit(
                            SharedPoisUiEvent.ShowMessage(
                                error.message ?: "Failed to share POI"
                            )
                        )
                    }
            } catch (e: Exception) {
                _createPoiState.update { it.copy(isUploading = false) }
                _uiEvents.emit(SharedPoisUiEvent.ShowMessage("Error: ${e.message}"))
            }
        }
    }

    fun shareExistingPoi(poiId: String, recipientId: String, message: String?) {
        scope.launch {
            // Check connectivity first
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.emit(SharedPoisUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            val request = SharePoiRequest(
                recipientId = recipientId,
                poiId = poiId,
                message = message?.trim()?.ifEmpty { null }
            )

            sharePoiUseCase(request)
                .onSuccess {
                    _uiEvents.emit(SharedPoisUiEvent.ShowMessage("POI shared successfully!"))
                    refresh()
                }
                .onFailure { error ->
                    _uiEvents.emit(
                        SharedPoisUiEvent.ShowMessage(
                            error.message ?: "Failed to share POI"
                        )
                    )
                }
        }
    }

    // Actions on Shared POIs

    fun markAsViewed(sharedPoi: SharedPoi) {
        if (sharedPoi.isViewed) return

        scope.launch {
            markSharedPoiViewedUseCase(sharedPoi.id)
                .onFailure { error ->
                    _uiEvents.emit(
                        SharedPoisUiEvent.ShowMessage(
                            error.message ?: "Failed to mark as viewed"
                        )
                    )
                }
        }
    }

    fun deleteSharedPoi(sharedPoi: SharedPoi) {
        scope.launch {
            // Check connectivity first
            if (!connectivityObserver.isNetworkAvailable()) {
                _uiEvents.emit(SharedPoisUiEvent.ShowMessage(offlineMessage))
                return@launch
            }

            deleteSharedPoiUseCase(sharedPoi.id)
                .onSuccess {
                    _uiEvents.emit(SharedPoisUiEvent.ShowMessage("Shared POI deleted"))
                    // Refresh data without showing the Loading state
                    refreshSilently()
                }
                .onFailure { error ->
                    _uiEvents.emit(
                        SharedPoisUiEvent.ShowMessage(
                            error.message ?: "Failed to delete"
                        )
                    )
                }
        }
    }

    /**
     * Refreshes data without changing to Loading state.
     * Used after delete operations to avoid UI flicker.
     */
    private suspend fun refreshSilently() {
        listOf(
            getReceivedSharedPoisUseCase.refresh(),
            getSentSharedPoisUseCase.refresh(),
            getUnviewedSharedPoisUseCase.refresh()
        )
        // Success/failure is handled by observeData() flow collection
    }

    fun navigateToPoiOnMap(sharedPoi: SharedPoi) {
        scope.launch {
            val coords = sharedPoi.coordinates
            if (coords != null) {
                _uiEvents.emit(SharedPoisUiEvent.NavigateToPoiOnMap(coords.first, coords.second))
            } else {
                _uiEvents.emit(
                    SharedPoisUiEvent.ShowMessage(
                        "Cannot navigate to this POI - location unavailable"
                    )
                )
            }
        }
    }

    /**
     * Shows a shared POI on the map without navigating to it.
     * Centers the map on the POI's location.
     */
    fun showPoiOnMap(sharedPoi: SharedPoi) {
        scope.launch {
            val coords = sharedPoi.coordinates
            if (coords != null) {
                _uiEvents.emit(SharedPoisUiEvent.NavigateToPoiOnMap(coords.first, coords.second))
            } else {
                _uiEvents.emit(
                    SharedPoisUiEvent.ShowMessage(
                        "Cannot show this POI on map - location unavailable"
                    )
                )
            }
        }
    }
}
