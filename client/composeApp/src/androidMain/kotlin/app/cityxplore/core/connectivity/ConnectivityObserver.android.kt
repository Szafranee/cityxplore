package app.cityxplore.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Android implementation of [ConnectivityObserver].
 *
 * Uses Android's ConnectivityManager to monitor network state changes
 * and emit updates via a Flow.
 */
actual class ConnectivityObserver(
    private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Observes network connectivity changes using ConnectivityManager.NetworkCallback.
     */
    actual fun observe(): Flow<NetworkStatus> = callbackFlow {
        // Emit the current status immediately
        trySend(getCurrentNetworkStatus())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Use aggregated status to avoid false states when multiple networks exist
                trySend(getCurrentNetworkStatus())
            }

            override fun onLost(network: Network) {
                // Use aggregated status - other networks may still be available
                trySend(getCurrentNetworkStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // Use aggregated status for accurate connectivity state
                trySend(getCurrentNetworkStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Checks if a network is currently available.
     */
    actual fun isNetworkAvailable(): Boolean = getCurrentNetworkStatus() == NetworkStatus.AVAILABLE

    private fun getCurrentNetworkStatus(): NetworkStatus {
        val network = connectivityManager.activeNetwork ?: return NetworkStatus.UNAVAILABLE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkStatus.UNAVAILABLE

        return if (
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            NetworkStatus.AVAILABLE
        } else {
            NetworkStatus.UNAVAILABLE
        }
    }
}
