package app.cityxplore.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

/**
 * Android implementation of [LocationService] using Google Play Services FusedLocationProviderClient.
 *
 * **Important:** Callers MUST request and obtain location permissions before using this service.
 * If permissions are not granted, [observeLocation] will return an empty Flow and
 * [getCurrentLocation] will return null.
 *
 * Required permissions:
 * - [Manifest.permission.ACCESS_FINE_LOCATION] (preferred)
 * - [Manifest.permission.ACCESS_COARSE_LOCATION] (minimum)
 */
class AndroidLocationService(
    private val context: Context
) : LocationService {
    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Checks if location permissions are granted.
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override fun observeLocation(): Flow<Location> {
        if (!hasLocationPermission()) {
            return emptyFlow()
        }

        return callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2000L)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        trySend(Location(it.latitude, it.longitude))
                    }
                }
            }

            try {
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            } catch (e: Exception) {
                close(e)
            }

            awaitClose {
                client.removeLocationUpdates(callback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            client.lastLocation.await()?.let {
                Location(it.latitude, it.longitude)
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
