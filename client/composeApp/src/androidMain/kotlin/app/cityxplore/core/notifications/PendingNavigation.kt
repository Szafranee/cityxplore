package app.cityxplore.core.notifications

import app.cityxplore.MainActivity
import app.cityxplore.database.applicationContext

/**
 * Android implementation - consumes pending navigation from SharedPreferences via MainActivity.
 * Uses the application context initialised in the database module.
 */
actual fun consumePendingNavigation(): String? {
    return try {
        MainActivity.consumeNavigation(applicationContext)
    } catch (_: UninitializedPropertyAccessException) {
        // Context not yet initialised
        null
    }
}
