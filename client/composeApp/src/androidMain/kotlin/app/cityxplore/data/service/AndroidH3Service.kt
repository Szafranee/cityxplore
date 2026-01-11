package app.cityxplore.data.service

import app.cityxplore.domain.service.H3Service
import com.uber.h3core.H3Core
import java.io.IOException

/**
 * Android implementation of [H3Service] using the official Uber H3 Java library.
 *
 * This service wraps the H3Core library and handles native library loading.
 * It provides methods for H3 indexing and geometric calculations.
 */
class AndroidH3Service : H3Service {
    private val h3: H3Core by lazy {
        try {
            // Try to load the library manually first if it's in the library path
            System.loadLibrary("h3-java")
            H3Core.newSystemInstance()
        } catch (_: UnsatisfiedLinkError) {
            // Fallback to extracting from resources
            try {
                H3Core.newInstance()
            } catch (e2: IOException) {
                throw RuntimeException("Failed to initialize H3Core", e2)
            } catch (e3: UnsatisfiedLinkError) {
                throw RuntimeException(
                    "Failed to load H3 native library. Ensure the APK contains the correct .so files.",
                    e3
                )
            }
        }
    }

    override fun latLngToCell(latitude: Double, longitude: Double, resolution: Int): Long {
        return h3.latLngToCell(latitude, longitude, resolution)
    }

    override fun cellToBoundary(h3Index: Long): List<Pair<Double, Double>> {
        return h3.cellToBoundary(h3Index).map { it.lat to it.lng }
    }

    override fun h3ToString(h3Index: Long): String {
        return h3.h3ToString(h3Index)
    }

    override fun stringToH3(h3String: String): Long {
        return h3.stringToH3(h3String)
    }

    override fun gridDisk(h3Index: Long, k: Int): List<Long> {
        return h3.gridDisk(h3Index, k)
    }
}
