package app.cityxplore.map.domain

/**
 * iOS stub implementation for hex calculation.
 *
 * TODO: Implement using H3 Swift library or bridge to native implementation.
 * For now, returns empty set to avoid compilation errors.
 */
internal actual object HexCalculator {
    actual fun calculateHexagonsToReveal(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        resolution: Int
    ): Set<String> {
        // TODO: Implement H3 for iOS
        println("WARNING: Fog of War not yet implemented for iOS")
        return emptySet()
    }
}
