package app.cityxplore.core.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Represents the current network connectivity status.
 */
enum class NetworkStatus {
    /** Network is available and connected */
    AVAILABLE,

    /** Network is unavailable or disconnected */
    UNAVAILABLE,

    /** Network status is being determined */
    UNKNOWN
}

/**
 * Platform-specific network connectivity observer.
 *
 * Provides a reactive stream of network status changes,
 * allowing the app to respond to connectivity changes
 * (e.g. triggering sync when the connection is restored).
 */
expect class ConnectivityObserver {
    /**
     * Observes network connectivity changes.
     *
     * @return Flow emitting [NetworkStatus] whenever connectivity changes.
     */
    fun observe(): Flow<NetworkStatus>

    /**
     * Checks if a network is currently available.
     *
     * @return true if a network is available, false otherwise.
     */
    fun isNetworkAvailable(): Boolean
}
