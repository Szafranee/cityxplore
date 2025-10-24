package org.cityxplore.backend.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Data Transfer Object representing a user's profile.
 *
 * This class is typically used for transferring profile-related user data
 * between layers or systems in the application.
 *
 * @property id Unique identifier for the user.
 * @property email User's email address.
 * @property username User's chosen username.
 * @property avatarUrl Optional URL pointing to the user's avatar image.
 * @property totalDistance Total distance traveled or associated with the user, represented in a unit such as kilometers.
 * @property totalPoisDiscovered Total number of Points of Interest (POIs) discovered by the user.
 * @property createdAt Timestamp representing when the user's profile was created.
 */
data class UserProfileDto(
    val id: UUID,
    val email: String,
    val username: String,
    val avatarUrl: String?,
    val totalDistance: BigDecimal,
    val totalPoisDiscovered: Int,
    val createdAt: LocalDateTime?
)
