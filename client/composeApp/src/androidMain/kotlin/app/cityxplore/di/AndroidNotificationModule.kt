package app.cityxplore.di

import app.cityxplore.core.notifications.AndroidNotificationService
import app.cityxplore.core.notifications.NotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module for notification dependencies.
 *
 * Provides AndroidNotificationService as the NotificationService implementation.
 */
fun androidNotificationModule() = module {
    single<NotificationService> {
        AndroidNotificationService(androidContext())
    }
}
