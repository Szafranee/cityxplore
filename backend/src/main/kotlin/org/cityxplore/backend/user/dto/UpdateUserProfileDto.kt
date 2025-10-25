package org.cityxplore.backend.user.dto

import jakarta.validation.constraints.Size

/**
 * Data Transfer Object used for updating a user's profile information.
 *
 * This DTO is typically used in user profile update requests where only specific
 * fields need to be modified.
 *
 * @property username Optional updated username for the user (1..200 chars when present).
 * @property avatarUrl Optional updated URL for the user's avatar image (max 500 chars).
 */
data class UpdateUserProfileDto(
    @field:Size(min = 1, max = 200)
    val username: String?,
    @field:Size(max = 500)
    val avatarUrl: String?
)
