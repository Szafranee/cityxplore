package app.cityxplore.social.presentation.sharedpois

import app.cityxplore.social.domain.DeleteSharedPoiUseCase
import app.cityxplore.social.domain.GetReceivedSharedPoisUseCase
import app.cityxplore.social.domain.GetSentSharedPoisUseCase
import app.cityxplore.social.domain.GetUnviewedSharedPoisUseCase
import app.cityxplore.social.domain.MarkSharedPoiViewedUseCase
import app.cityxplore.social.domain.SharePoiUseCase
import app.cityxplore.social.domain.model.CustomPoi
import app.cityxplore.social.domain.model.SharePoiRequest
import app.cityxplore.social.domain.model.SharedPoi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Shared POIs UI state and user interactions.
 */
class SharedPoisViewModel(
    private val getSentSharedPoisUseCase: GetSentSharedPoisUseCase,
    private val getReceivedSharedPoisUseCase: GetReceivedSharedPoisUseCase,
    private val getUnviewedSharedPoisUseCase: GetUnviewedSharedPoisUseCase,
    private val sharePoiUseCase: SharePoiUseCase,
    private val markSharedPoiViewedUseCase: MarkSharedPoiViewedUseCase,
    private val deleteSharedPoiUseCase: DeleteSharedPoiUseCase,
    private val sharedPoiRepository: app.cityxplore.social.domain.repository.SharedPoiRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow<SharedPoisUiState>(SharedPoisUiState.Loading)
    val uiState: StateFlow<SharedPoisUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<SharedPoisUiEvent>()
    val uiEvents: SharedFlow<SharedPoisUiEvent> = _uiEvents.asSharedFlow()

    private val _createPoiState = MutableStateFlow(CreateCustomPoiState())
    val createPoiState: StateFlow<CreateCustomPoiState> = _createPoiState.asStateFlow()

    private val _isShareDialogVisible = MutableStateFlow(false)
    val isShareDialogVisible: StateFlow<Boolean> = _isShareDialogVisible.asStateFlow()

    private val _pendingSharePoi = MutableStateFlow<CustomPoi?>(null)

    init {
        observeData()
        refresh()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                getReceivedSharedPoisUseCase(),
                getSentSharedPoisUseCase(),
                getUnviewedSharedPoisUseCase.count()
            ) { received, sent, unviewedCount ->
                SharedPoisUiState.Content(
                    receivedPois = received,
                    sentPois = sent,
                    unviewedCount = unviewedCount
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = SharedPoisUiState.Loading

            val results = listOf(
                getReceivedSharedPoisUseCase.refresh(),
                getSentSharedPoisUseCase.refresh(),
                getUnviewedSharedPoisUseCase.refresh()
            )

            val firstFailure = results.firstOrNull { it.isFailure }
            if (firstFailure != null) {
                _uiState.value = SharedPoisUiState.Error(
                    firstFailure.exceptionOrNull()?.message ?: "Failed to load shared POIs"
                )
            }
            // Success case is handled by observeData()
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

    fun updateCreatePoiImage(imageUrl: String?) {
        _createPoiState.update {
            it.copy(imageUrls = if (imageUrl != null) listOf(imageUrl) else emptyList())
        }
    }

    fun onImagePicked(bytes: ByteArray?) {
        if (bytes == null) return

        viewModelScope.launch {
            // Optimistically we could show a loading state on the image field, but for now just upload
            sharedPoiRepository.uploadPoiImage(bytes)
                .onSuccess { url ->
                    updateCreatePoiImage(url)
                }
                .onFailure { error ->
                    _uiEvents.emit(
                        SharedPoisUiEvent.ShowMessage(
                            "Failed to upload image: ${error.message}"
                        )
                    )
                }
        }
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

    fun proceedToShareDialog() {
        val customPoi = _createPoiState.value.toCustomPoi()
        if (customPoi != null) {
            _pendingSharePoi.value = customPoi
            _isShareDialogVisible.value = true
        }
    }

    // Share Dialog

    fun hideShareDialog() {
        _isShareDialogVisible.value = false
        _pendingSharePoi.value = null
    }

    fun shareCustomPoi(recipientId: String, message: String?) {
        val customPoi = _pendingSharePoi.value ?: return

        // Hide dialog immediately to prevent multiple clicks
        hideShareDialog()

        viewModelScope.launch {
            val request = SharePoiRequest(
                recipientId = recipientId,
                customPoi = customPoi,
                message = message?.trim()?.ifEmpty { null }
            )

            sharePoiUseCase(request)
                .onSuccess {
                    _uiEvents.emit(SharedPoisUiEvent.ShowMessage("POI shared successfully!"))
                    _uiEvents.emit(SharedPoisUiEvent.ShareSuccess)
                    resetCreatePoiState()
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

    fun shareExistingPoi(poiId: String, recipientId: String, message: String?) {
        viewModelScope.launch {
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

        viewModelScope.launch {
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
        viewModelScope.launch {
            deleteSharedPoiUseCase(sharedPoi.id)
                .onSuccess {
                    _uiEvents.emit(SharedPoisUiEvent.ShowMessage("Shared POI deleted"))
                    refresh()
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

    fun navigateToPoiOnMap(sharedPoi: SharedPoi) {
        viewModelScope.launch {
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
}
