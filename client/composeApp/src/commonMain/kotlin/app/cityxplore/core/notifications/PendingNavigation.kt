package app.cityxplore.core.notifications

/**
 * Platform-specific function to consume pending navigation from notification click.
 * Returns the navigation target (e.g. "friends", "shared_pois") or null if none are pending.
 *
 * After calling this function, the pending navigation is cleared.
 */
expect fun consumePendingNavigation(): String?
