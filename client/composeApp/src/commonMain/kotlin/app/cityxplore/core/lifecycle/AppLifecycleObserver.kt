package app.cityxplore.core.lifecycle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/**
 * Represents the application lifecycle state.
 */
enum class AppLifecycleState {
    /** App is in the foreground and active */
    FOREGROUND,

    /** App is in the background */
    BACKGROUND,

    /** App was just resumed from the background */
    RESUMED
}

/**
 * Observes application lifecycle events to optimise data loading.
 *
 * This prevents unnecessary data reloads when:
 * - User briefly switches to another app and returns
 * - User checks a notification and comes back
 * - Screen is locked and unlocked
 *
 * The key optimisation is tracking the time spent in the background:
 * - Short background time (< threshold): Don't reload, use cached data
 * - Long background time (> threshold): Trigger background refresh
 */
class AppLifecycleObserver {
    private val _lifecycleState = MutableStateFlow(AppLifecycleState.FOREGROUND)
    val lifecycleState: Flow<AppLifecycleState> = _lifecycleState.asStateFlow()
    private var lastBackgroundTime: Long = 0
    private var wasInBackground: Boolean = false

    companion object {
        /** Time in milliseconds after which we consider data potentially stale */
        const val BACKGROUND_THRESHOLD_MS = 60_000L // 1 minute

        /** Time in milliseconds for a "quick switch" - no reload needed */
        const val QUICK_SWITCH_THRESHOLD_MS = 5_000L // 5 seconds
    }

    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Called when app enters background.
     */
    fun onBackground() {
        lastBackgroundTime = currentTimeMillis()
        wasInBackground = true
        _lifecycleState.value = AppLifecycleState.BACKGROUND
    }

    /**
     * Called when app returns to foreground.
     */
    fun onForeground() {
        _lifecycleState.value = if (wasInBackground) {
            AppLifecycleState.RESUMED
        } else {
            AppLifecycleState.FOREGROUND
        }
        wasInBackground = false
    }

    /**
     * Calculates how long the app was in the background.
     *
     * @return Time in milliseconds spent in the background, or 0 if not applicable.
     */
    fun getBackgroundDuration(): Long {
        if (lastBackgroundTime == 0L) return 0
        return currentTimeMillis() - lastBackgroundTime
    }

    /**
     * Determines if data should be refreshed based on background duration.
     *
     * @return true if the app was in the background long enough to warrant a refresh.
     */
    fun shouldRefreshOnResume(): Boolean {
        return getBackgroundDuration() > BACKGROUND_THRESHOLD_MS
    }

    /**
     * Determines if this was a quick app switch (no refresh needed).
     *
     * @return true if the user just quickly switched apps and came back.
     */
    fun wasQuickSwitch(): Boolean {
        return getBackgroundDuration() < QUICK_SWITCH_THRESHOLD_MS
    }

    /**
     * Resets the background tracking state.
     */
    fun reset() {
        lastBackgroundTime = 0
        wasInBackground = false
        _lifecycleState.value = AppLifecycleState.FOREGROUND
    }
}
