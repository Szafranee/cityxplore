package app.cityxplore.di

import app.cityxplore.core.notifications.SocialNotificationManager
import org.koin.dsl.module

/**
 * Koin module for notification-related dependencies.
 *
 * Provides:
 * - NotificationService: Platform-specific notification implementation
 * - SocialNotificationManager: Manages social notification events via Supabase Realtime
 *
 * Note: NotificationService implementation is provided by platform-specific modules
 * (AndroidNotificationModule for Android, IosNotificationModule for iOS)
 */
fun notificationModule() = module {
    // SocialNotificationManager - listens for real-time social events via Supabase Realtime
    // Receives instant notifications for friend requests, accepted friendships, and shared POIs
    single {
        SocialNotificationManager(
            supabaseClient = get(),
            notificationService = get(),
            authRepository = get(),
            dispatchers = get()
        )
    }
}
