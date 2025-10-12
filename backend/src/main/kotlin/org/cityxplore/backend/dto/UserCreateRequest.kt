package org.cityxplore.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * DTO for creating a new User via API.
 */
data class UserCreateRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val username: String,
    val avatarUrl: String? = null
)
