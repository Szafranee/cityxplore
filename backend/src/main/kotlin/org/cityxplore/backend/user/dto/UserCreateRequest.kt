package org.cityxplore.backend.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO for creating a new User via API.
 */
data class UserCreateRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 320)
    val email: String,
    @field:NotBlank
    @field:Size(min = 1, max = 200)
    val username: String,
    @field:Size(max = 500)
    val avatarUrl: String? = null
)
