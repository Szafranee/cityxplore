package org.cityxplore.backend.mapper

import org.cityxplore.backend.dto.UserCreateRequest
import org.cityxplore.backend.dto.UserResponseDto
import org.cityxplore.backend.entity.User

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
