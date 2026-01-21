package app.cityxplore.core.notifications

import androidx.compose.runtime.Composable

/**
 * Platform-specific composable that requests notification permission.
 *
 * - Android: Requests POST_NOTIFICATIONS permission on Android 13+
 * - iOS: Placeholder (no-op)
 */
@Composable
expect fun RequestNotificationPermission()
