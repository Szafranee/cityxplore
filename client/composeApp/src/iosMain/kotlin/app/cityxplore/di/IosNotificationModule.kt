package app.cityxplore.di

import app.cityxplore.core.notifications.IosNotificationService
import app.cityxplore.core.notifications.NotificationService
import org.koin.dsl.module

/**
 * iOS-specific Koin module for notification dependencies.
 *
 * Provides IosNotificationService (placeholder) as the NotificationService implementation.
 */
fun iosNotificationModule() = module {
    single<NotificationService> {
        IosNotificationService()
    }
}
