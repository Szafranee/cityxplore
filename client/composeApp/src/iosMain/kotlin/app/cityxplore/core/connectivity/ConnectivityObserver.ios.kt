package app.cityxplore.core.connectivity

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue
import kotlin.concurrent.Volatile

/**
 * iOS implementation of [ConnectivityObserver].
 *
 * Uses NWPathMonitor to monitor network state changes
 * and emit updates via a Flow.
 */
actual class ConnectivityObserver {
    private val monitor = nw_path_monitor_create()

    @Volatile
    private var currentStatus: NetworkStatus = NetworkStatus.UNKNOWN

    /**
     * Observes network connectivity changes using NWPathMonitor.
     */
    actual fun observe(): Flow<NetworkStatus> = callbackFlow {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = if (nw_path_get_status(path) == nw_path_status_satisfied) {
                NetworkStatus.AVAILABLE
            } else {
                NetworkStatus.UNAVAILABLE
            }
            currentStatus = status
            trySend(status)
        }

        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)

        awaitClose {
            nw_path_monitor_cancel(monitor)
        }
    }.distinctUntilChanged()

    /**
     * Checks if a network is currently available.
     */
    actual fun isNetworkAvailable(): Boolean = currentStatus == NetworkStatus.AVAILABLE
}
