package org.cityxplore.backend.poi.entity

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class PoiImage(
    val url: String? = null,

    @JsonProperty("photo_reference")
    val photoReference: String? = null,

    val source: String? = null,
    val author: String? = null,
    val license: String? = null,
    val attributions: String? = null
) : Serializable
