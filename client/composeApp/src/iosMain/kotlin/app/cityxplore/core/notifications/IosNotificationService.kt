package app.cityxplore.core.notifications

/**
 * iOS placeholder implementation of [NotificationService].
 *
 * TODO: Implement using UNUserNotificationCenter for actual iOS notifications.
 * Currently just logs notification events for debugging.
 */
class IosNotificationService : NotificationService {

    override fun showSharedPoiNotification(
        sharerName: String,
        poiName: String,
        message: String?
    ) {
        // Placeholder - log for now
        println("iOS Notification [Shared POI]: $sharerName shared '$poiName'${message?.let { " - $it" } ?: ""}")
    }

    override fun showFriendRequestNotification(fromUsername: String) {
        // Placeholder - log for now
        println("iOS Notification [Friend Request]: $fromUsername wants to be your friend")
    }

    override fun showFriendRequestAcceptedNotification(username: String) {
        // Placeholder - log for now
        println("iOS Notification [Friend Accepted]: $username accepted your friend request")
    }

    override fun areNotificationsEnabled(): Boolean {
        // Placeholder - always return false until implemented
        return false
    }
}
