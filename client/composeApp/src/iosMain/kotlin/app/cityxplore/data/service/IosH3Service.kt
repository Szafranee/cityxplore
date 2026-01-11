package app.cityxplore.data.service

import app.cityxplore.domain.service.H3Service

class IosH3Service : H3Service {
    // TODO: Implement H3 for iOS using native library or wrapper
    // For now, this is a stub to allow compilation on non-Mac environments

    override fun latLngToCell(latitude: Double, longitude: Double, resolution: Int): Long {
        println("H3Service.latLngToCell not implemented on iOS")
        return 0L
    }

    override fun cellToBoundary(h3Index: Long): List<Pair<Double, Double>> {
        return emptyList()
    }

    override fun h3ToString(h3Index: Long): String {
        return h3Index.toString(16)
    }

    override fun stringToH3(h3String: String): Long {
        return h3String.toLongOrNull(16) ?: 0L
    }

    override fun gridDisk(h3Index: Long, k: Int): List<Long> {
        return emptyList()
    }
}
