package app.cityxplore.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.cityxplore.MainActivity

/**
 * Android implementation of [NotificationService].
 *
 * Uses Android's NotificationManager with notification channels for Android 8.0+.
 * Handles permission checks for Android 13+ (POST_NOTIFICATIONS permission).
 */
class AndroidNotificationService(
    private val context: Context
) : NotificationService {

    companion object {
        const val CHANNEL_SOCIAL = "cityxplore_social"
        const val CHANNEL_SOCIAL_NAME = "Social Notifications"

        // Navigation extras
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val NAVIGATE_TO_FRIENDS = "friends"
        const val NAVIGATE_TO_SHARED_POIS = "shared_pois"

        private var notificationId = 1000
    }

    init {
        createNotificationChannels()
    }

    /**
     * Creates notification channels required for Android 8.0+.
     * Uses IMPORTANCE_HIGH for heads-up notifications (banners).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL,
                CHANNEL_SOCIAL_NAME,
                NotificationManager.IMPORTANCE_HIGH // HIGH for heads-up banner notifications
            ).apply {
                description = "Notifications for friend requests and shared POIs"
                enableVibration(true)
                enableLights(true)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(socialChannel)
        }
    }

    override fun showSharedPoiNotification(
        sharerName: String,
        poiName: String,
        message: String?
    ) {
        val title = "$sharerName shared a place with you"
        val text = if (message.isNullOrBlank()) poiName else "$poiName: $message"

        showNotification(
            title = title,
            text = text,
            notificationId = notificationId++,
            navigateTo = NAVIGATE_TO_SHARED_POIS
        )
    }

    override fun showFriendRequestNotification(fromUsername: String) {
        showNotification(
            title = "New Friend Request",
            text = "$fromUsername wants to be your friend",
            notificationId = notificationId++,
            navigateTo = NAVIGATE_TO_FRIENDS
        )
    }

    override fun showFriendRequestAcceptedNotification(username: String) {
        showNotification(
            title = "Friend Request Accepted",
            text = "$username accepted your friend request! 🎉",
            notificationId = notificationId++,
            navigateTo = NAVIGATE_TO_FRIENDS
        )
    }

    override fun areNotificationsEnabled(): Boolean {
        // Check if notifications are enabled at system level
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }

        // For Android 13+, check POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        return true
    }

    /**
     * Shows a notification with the given title and text.
     */
    @SuppressLint("MissingPermission") // Permission is checked in areNotificationsEnabled()
    private fun showNotification(
        title: String,
        text: String,
        notificationId: Int,
        navigateTo: String
    ) {
        if (!areNotificationsEnabled()) {
            println("Notifications not enabled, skipping: $title")
            return
        }

        // Create intent to open the app when notification is tapped
        // Use SINGLE_TOP to prevent creating new activity if already running
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, navigateTo)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with app icon
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // HIGH for heads-up banner
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            println("SecurityException when posting notification: ${e.message}")
        }
    }
}
