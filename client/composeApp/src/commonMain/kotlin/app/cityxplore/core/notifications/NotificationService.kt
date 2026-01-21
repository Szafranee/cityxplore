package app.cityxplore.core.notifications

/**
 * Platform-agnostic interface for displaying system notifications.
 *
 * Implementations:
 * - Android: Uses NotificationManager with channels
 * - iOS: Placeholder (not implemented)
 */
interface NotificationService {

    /**
     * Shows a notification when a friend shares a POI with the user.
     *
     * @param sharerName Name of the friend who shared the POI
     * @param poiName Name of the shared POI (or custom POI name)
     * @param message Optional message from the sharer
     */
    fun showSharedPoiNotification(
        sharerName: String,
        poiName: String,
        message: String?
    )

    /**
     * Shows a notification when a friend request is received.
     *
     * @param fromUsername Username of the person sending the request
     */
    fun showFriendRequestNotification(fromUsername: String)

    /**
     * Shows a notification when a friend request is accepted.
     *
     * @param username Username of the person who accepted the request
     */
    fun showFriendRequestAcceptedNotification(username: String)

    /**
     * Checks if notifications are enabled/permitted on this device.
     */
    fun areNotificationsEnabled(): Boolean
}
