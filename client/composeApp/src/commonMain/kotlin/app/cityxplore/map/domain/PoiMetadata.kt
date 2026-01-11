package app.cityxplore.map.domain

/**
 * Domain model representing metadata associated with a POI.
 * Contains optional fields like trivia, opening hours, pricing, etc.
 *
 * @property trivia A "Did you know?" fact about the place.
 * @property openingHours Structured opening hours information.
 * @property visitDuration Estimated time to visit (e.g. "1-2h").
 * @property isFree Whether entry is free.
 * @property website URL to the official website.
 * @property address Physical address.
 * @property buildYear Year of construction or era.
 */
data class PoiMetadata(
    val trivia: String? = null,
    val openingHours: OpeningHours? = null,
    val visitDuration: String? = null,
    val isFree: Boolean? = null,
    val website: String? = null,
    val address: String? = null,
    val buildYear: String? = null
)

/**
 * Represents structured opening hours.
 *
 * @property openNow Whether the place is currently open.
 * @property weekdayText List of strings representing opening hours for each day (e.g. "Monday: 9:00 AM – 5:00 PM").
 */
data class OpeningHours(
    val openNow: Boolean? = null,
    val weekdayText: List<String> = emptyList()
)
