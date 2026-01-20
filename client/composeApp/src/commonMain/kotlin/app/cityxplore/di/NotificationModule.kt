package app.cityxplore.di

import app.cityxplore.core.notifications.SocialNotificationManager
import org.koin.dsl.module

/**
 * Koin module for notification-related dependencies.
 *
 * Provides:
 * - NotificationService: Platform-specific notification implementation
 * - SocialNotificationManager: Manages social notification events with polling
 *
 * Note: NotificationService implementation is provided by platform-specific modules
 * (AndroidNotificationModule for Android, IosNotificationModule for iOS)
 */
fun notificationModule() = module {
    // SocialNotificationManager - observes social data and triggers notifications
    // Also polls for new data every 30 seconds when online
    single {
        SocialNotificationManager(
            notificationService = get(),
            sharedPoiRepository = get(),
            socialRepository = get(),
            connectivityObserver = get(),
            dispatchers = get()
        )
    }
}
