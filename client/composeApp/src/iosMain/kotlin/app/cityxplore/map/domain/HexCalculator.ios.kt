package app.cityxplore.map.domain

internal actual object HexCalculator {
    actual fun calculateHexagonsToReveal(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        resolution: Int
    ): Set<String> {
        // TODO: Implement H3 for iOS (using C interop or binding)
        return emptySet()
    }
}
