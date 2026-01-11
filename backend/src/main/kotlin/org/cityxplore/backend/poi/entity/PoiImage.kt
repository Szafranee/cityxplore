package org.cityxplore.backend.poi.entity

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.URL
import java.io.Serializable

/**
 * Represents an image associated with a Point of Interest (POI).
 * This class handles images from various sources, including direct URLs
 * (e.g. Wikimedia) and API references (e.g. Google Places).
 *
 * @property url Direct URL to the image (if available).
 * @property photoReference ID/Reference for fetching the image from an external API (e.g. Google Places).
 * @property source The source of the image (e.g. "Wikimedia Commons", "Google Places").
 * @property author The author or creator of the image (for attribution).
 * @property license The licence under which the image is provided (e.g. "CC BY-SA 4.0").
 * @property attributions HTML attributions required by the provider (specifically for Google Places).
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
