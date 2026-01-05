package app.cityxplore.domain.service

interface H3Service {
    /**
     * Converts latitude and longitude to H3 index at specified resolution.
     */
    fun latLngToCell(latitude: Double, longitude: Double, resolution: Int): Long

    /**
     * Returns the boundary of the H3 cell as a list of (lat, lng) pairs.
     */
    fun cellToBoundary(h3Index: Long): List<Pair<Double, Double>>

    /**
     * Returns the H3 index string representation.
     */
    fun h3ToString(h3Index: Long): String

    /**
     * Returns the H3 index from string representation.
     */
    fun stringToH3(h3String: String): Long

    /**
     * Returns all cells within k distance of the origin cell (k-ring).
     */
    fun gridDisk(h3Index: Long, k: Int): List<Long>
}
