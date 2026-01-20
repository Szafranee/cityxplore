package app.cityxplore.core.notifications

import app.cityxplore.core.CityXploreDispatchers
import app.cityxplore.core.connectivity.ConnectivityObserver
import app.cityxplore.social.domain.model.Friendship
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.domain.repository.SharedPoiRepository
import app.cityxplore.social.domain.repository.SocialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages social notifications by periodically polling for new data
 * and showing notifications when new items are detected.
 *
 * Detects:
 * - New shared POIs received from friends
 * - New friend requests
 * - Accepted friend requests (when a friend list grows)
 *
 * Uses direct polling instead of Flow observation to work even when
 * the user is not on the Friends screen.
 *
 * Polling interval: 30 seconds when online.
 */
class SocialNotificationManager(
    private val notificationService: NotificationService,
    private val sharedPoiRepository: SharedPoiRepository,
    private val socialRepository: SocialRepository,
    private val connectivityObserver: ConnectivityObserver,
    dispatchers: CityXploreDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    companion object {
        /** Polling interval for checking new notifications (30 seconds) */
        private const val POLLING_INTERVAL_MS = 30_000L

        /** Initial delay before the first poll (5 seconds) */
        private const val INITIAL_DELAY_MS = 5_000L
    }

    // Track the previous state to detect new items (by ID)
    private var lastReceivedPoiIds: Set<String> = emptySet()
    private var lastPendingRequestIds: Set<String> = emptySet()
    private var lastFriendIds: Set<String> = emptySet()

    // Store data for notification content
    private var lastReceivedPois: Map<String, SharedPoi> = emptyMap()
    private var lastPendingRequests: Map<String, Friendship> = emptyMap()
    private var lastFriends: Map<String, Friendship> = emptyMap()

    private var pollingJob: Job? = null
    private var isInitialized = false

    /**
     * Starts periodic polling for new social data.
     * Should be called after the user is authenticated.
     */
    fun startObserving() {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch {
            // Initial delay to let the app load first
            delay(INITIAL_DELAY_MS)

            // Do an initial load without notifications
            initializeState()

            while (isActive) {
                delay(POLLING_INTERVAL_MS)

                try {
                    if (connectivityObserver.isNetworkAvailable()) {
                        checkForNewNotifications()
                    }
                } catch (e: Exception) {
                    println("SocialNotificationManager: Polling error: ${e.message}")
                }
            }
        }
    }

    /**
     * Stops polling for social data.
     * Should be called on logout.
     */
    fun stopObserving() {
        pollingJob?.cancel()
        pollingJob = null
        isInitialized = false

        // Reset state
        lastReceivedPoiIds = emptySet()
        lastPendingRequestIds = emptySet()
        lastFriendIds = emptySet()
        lastReceivedPois = emptyMap()
        lastPendingRequests = emptyMap()
        lastFriends = emptyMap()
    }

    /**
     * Initialises state without showing notifications.
     * Called on the first load to establish a baseline.
     */
    private suspend fun initializeState() {
        try {
            // Refresh all data
            sharedPoiRepository.refreshReceivedPois()
            socialRepository.refreshPendingRequests()
            socialRepository.refreshFriends()

            // Get the current state and store it
            val receivedPois = sharedPoiRepository.getReceivedPois().first()
            lastReceivedPois = receivedPois.associateBy { it.id }
            lastReceivedPoiIds = lastReceivedPois.keys

            val pendingRequests = socialRepository.getPendingRequests().first()
            lastPendingRequests = pendingRequests.associateBy { it.id }
            lastPendingRequestIds = lastPendingRequests.keys

            val friends = socialRepository.getFriends().first()
            lastFriends = friends.associateBy { it.otherUserId ?: it.id }
            lastFriendIds = lastFriends.keys

            isInitialized = true
            println("SocialNotificationManager: Initialized with ${lastReceivedPoiIds.size} POIs, ${lastPendingRequestIds.size} requests, ${lastFriendIds.size} friends")
        } catch (e: Exception) {
            println("SocialNotificationManager: Init error: ${e.message}")
        }
    }

    /**
     * Checks for new social data and shows notifications for new items.
     */
    private suspend fun checkForNewNotifications() {
        if (!isInitialized) {
            initializeState()
            return
        }

        try {
            // Refresh all data from the server
            sharedPoiRepository.refreshReceivedPois()
            socialRepository.refreshPendingRequests()
            socialRepository.refreshFriends()

            // Check for new shared POIs
            checkNewSharedPois()

            // Check for new friend requests
            checkNewFriendRequests()

            // Check for newly accepted friends
            checkNewFriends()

        } catch (e: Exception) {
            println("SocialNotificationManager: Check error: ${e.message}")
        }
    }

    /**
     * Checks for new shared POIs and shows notifications.
     */
    private suspend fun checkNewSharedPois() {
        val currentPois = sharedPoiRepository.getReceivedPois().first()
        val currentPoisMap = currentPois.associateBy { it.id }
        val currentIds = currentPoisMap.keys

        val newIds = currentIds - lastReceivedPoiIds

        newIds.forEach { newId ->
            val poi = currentPoisMap[newId]
            if (poi != null) {
                val poiName = poi.customPoi?.name
                    ?: poi.poiId?.let { "A place" }
                    ?: "A location"

                notificationService.showSharedPoiNotification(
                    sharerName = poi.sharerName ?: "A friend",
                    poiName = poiName,
                    message = poi.message
                )
                println("SocialNotificationManager: Notified new shared POI from ${poi.sharerName}")
            }
        }

        // Update tracked state
        lastReceivedPois = currentPoisMap
        lastReceivedPoiIds = currentIds
    }

    /**
     * Checks for new friend requests and shows notifications.
     */
    private suspend fun checkNewFriendRequests() {
        val currentRequests = socialRepository.getPendingRequests().first()
        val currentRequestsMap = currentRequests.associateBy { it.id }
        val currentIds = currentRequestsMap.keys

        val newIds = currentIds - lastPendingRequestIds

        newIds.forEach { newId ->
            val request = currentRequestsMap[newId]
            if (request != null) {
                notificationService.showFriendRequestNotification(
                    fromUsername = request.otherUserName ?: "Someone"
                )
                println("SocialNotificationManager: Notified new friend request from ${request.otherUserName}")
            }
        }

        // Update tracked state
        lastPendingRequests = currentRequestsMap
        lastPendingRequestIds = currentIds
    }

    /**
     * Checks for newly accepted friends and shows notifications.
     */
    private suspend fun checkNewFriends() {
        val currentFriends = socialRepository.getFriends().first()
        val currentFriendsMap = currentFriends.associateBy { it.otherUserId ?: it.id }
        val currentIds = currentFriendsMap.keys

        val newIds = currentIds - lastFriendIds

        newIds.forEach { newId ->
            val friend = currentFriendsMap[newId]
            if (friend != null) {
                notificationService.showFriendRequestAcceptedNotification(
                    username = friend.otherUserName ?: "A user"
                )
                println("SocialNotificationManager: Notified new friend ${friend.otherUserName}")
            }
        }

        // Update tracked state
        lastFriends = currentFriendsMap
        lastFriendIds = currentIds
    }
}
