package app.cityxplore.social.domain.model

/**
 * Domain model representing a Point of Interest shared between users.
 *
 * A SharedPoi can reference either:
 * - An existing POI from the main database (poiId is set, customPoi is null)
 * - A custom POI created by the sharer (poiId is null, customPoi is set)
 *
 * @property id Unique identifier of the shared POI record.
 * @property sharerId UUID of the user who shared the POI.
 * @property recipientId UUID of the user receiving the shared POI.
 * @property poiId Optional UUID of an existing POI being shared.
 * @property customPoi Optional custom POI data if sharing a user-created POI.
 * @property message Optional message accompanying the shared POI.
 * @property sharedAt ISO timestamp when the POI was shared.
 * @property viewedAt ISO timestamp when the recipient viewed the POI, null if not yet viewed.
 * @property sharerName Display the name of the user who shared the POI.
 * @property sharerAvatar Avatar URL of the user who shared the POI.
 * @property recipientName Display the name of the recipient (for sent POIs).
 * @property recipientAvatar Avatar URL of the recipient (for sent POIs).
 */
data class SharedPoi(
    val id: String,
    val sharerId: String,
    val recipientId: String,
    val poiId: String?,
    val customPoi: CustomPoi?,
    val message: String?,
    val sharedAt: String,
    val viewedAt: String?,
    val discoveredAt: String? = null,
    val sharerName: String? = null,
    val sharerAvatar: String? = null,
    val recipientName: String? = null,
    val recipientAvatar: String? = null
) {
    /**
     * Convenience flag indicating if this is a custom POI.
     * Derived from customPoi presence to ensure it stays consistent after copy().
     */
    val isCustomPoi: Boolean
        get() = customPoi != null

    /**
     * Returns the display name for this shared POI.
     * For custom POIs, uses the custom POI name.
     * For existing POIs, returns null (caller should resolve from the POI database).
     */
    val displayName: String?
        get() = customPoi?.name

    /**
     * Returns the coordinates if this is a custom POI.
     */
    val coordinates: Pair<Double, Double>?
        get() = customPoi?.let { Pair(it.latitude, it.longitude) }

    /**
     * Indicates whether this shared POI has been viewed by the recipient.
     */
    val isViewed: Boolean
        get() = viewedAt != null

    /**
     * Indicates whether this shared POI has been discovered (recipient got close to the location).
     */
    val isDiscovered: Boolean
        get() = discoveredAt != null
}

/**
 * Request model for sharing a POI with another user.
 */
data class SharePoiRequest(
    val recipientId: String,
    val poiId: String? = null,
    val customPoi: CustomPoi? = null,
    val message: String? = null
) {
    init {
        require((poiId != null) xor (customPoi != null)) {
            "Exactly one of poiId or customPoi must be provided"
        }
    }
}
