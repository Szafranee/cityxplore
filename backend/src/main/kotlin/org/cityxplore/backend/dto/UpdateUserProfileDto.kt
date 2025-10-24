package org.cityxplore.backend.dto

/**
 * Data Transfer Object used for updating a user's profile information.
 *
 * This DTO is typically used in user profile update requests where only specific
 * fields need to be modified.
 *
 * @property username Optional updated username for the user.
 * @property avatarUrl Optional updated URL for the user's avatar image.
 */
data class UpdateUserProfileDto(
    val username: String?,
    val avatarUrl: String?
)
