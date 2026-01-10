package org.cityxplore.backend.user.dto

import jakarta.validation.constraints.Size

/**
 * Request used for updating a user's profile information.
 * Only non-null fields will be applied.
 */
data class UpdateUserProfileRequest(
    @field:Size(min = 1, max = 200)
    val username: String?
)
