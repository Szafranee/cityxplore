package org.cityxplore.backend.user.mapper

import org.cityxplore.backend.user.dto.UserCreateRequest
import org.cityxplore.backend.user.dto.UserProfileResponse
import org.cityxplore.backend.user.dto.UserResponse
import org.cityxplore.backend.user.entity.User

fun User.toUserResponse(): UserResponse = UserResponse(
    id = this.id,
    email = this.email,
    username = this.username,
    avatarUrl = this.avatarUrl,
    createdAt = this.createdAt,
    lastActiveAt = this.lastActiveAt,
    totalDistance = this.totalDistance,
    totalPoisDiscovered = this.totalPoisDiscovered
)

fun List<User>.toUserResponseList(): List<UserResponse> = this.map { it.toUserResponse() }

fun UserCreateRequest.toEntity(): User = User(
    email = this.email,
    username = this.username,
    avatarUrl = this.avatarUrl
)

fun User.toDto() = UserProfileResponse(
    id = requireNotNull(id) { "Cannot map transient User entity to DTO" },
    email = email,
    username = username,
    avatarUrl = avatarUrl,
    totalDistance = totalDistance,
    totalPoisDiscovered = totalPoisDiscovered,
    createdAt = createdAt
)
