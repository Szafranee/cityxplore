package org.cityxplore.backend.poi.entity

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.URL
import java.io.Serializable

/**
 * Represents an image associated with a Point of Interest (POI).
 *
 * This class handles images from various sources. The structure varies based on [source]:
 *
 * **Wikimedia Commons** (preferred):
 * ```json
 * {
 *   "source": "Wikimedia Commons",
 *   "url": "https://upload.wikimedia.org/...",
 *   "author": "Author Name",
 *   "license": "CC BY-SA 4.0"
 * }
 * ```
 *
 * **Google Places API**:
 * ```json
 * {
 *   "source": "Google Places",
 *   "photo_reference": "AcnlKN30DEJ5...",
 *   "attributions": "<a href='...'>Attribution</a>"
 * }
 * ```
 *
 * **Legacy/Unknown**:
 * ```json
 * { "url": "https://example.com/photo.jpg" }
 * ```
 *
 * @property url Direct URL to the image (required for Wikimedia, optional for Google Places).
 * @property photoReference Google Places photo reference token (client generates URL with an API key).
 * @property source Source identifier: "Wikimedia Commons", "Google Places", "User Upload", or null.
 * @property author Author/creator name for attribution (recommended for Wikimedia).
 * @property license License identifier, e.g. "CC BY-SA 4.0", "CC0", "Public Domain".
 * @property attributions HTML attribution string required by Google Places API.
 *
 * @see org.cityxplore.backend.poi.entity.PointOfInterest for parent entity.
 */
data class PoiImage(
    @field:URL
    val url: String? = null,

    @JsonProperty("photo_reference")
    val photoReference: String? = null,

    val source: String? = null,
    val author: String? = null,
    val license: String? = null,
    val attributions: String? = null
) : Serializable
