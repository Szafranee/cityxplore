package org.cityxplore.backend.user.mapper

import org.cityxplore.backend.user.dto.UserCreateRequest
import org.cityxplore.backend.user.dto.UserResponseDto
import org.cityxplore.backend.user.entity.User

fun User.toUserResponseDto(): UserResponseDto = UserResponseDto(
    id = this.id,
    email = this.email,
    username = this.username,
    avatarUrl = this.avatarUrl,
    createdAt = this.createdAt,
    lastActiveAt = this.lastActiveAt,
    totalDistance = this.totalDistance,
    totalPoisDiscovered = this.totalPoisDiscovered
)

fun List<User>.toUserResponseDtoList(): List<UserResponseDto> = this.map { it.toUserResponseDto() }

fun UserCreateRequest.toEntity(): User = User(
    email = this.email,
    username = this.username,
    avatarUrl = this.avatarUrl
)
