package org.cityxplore.backend.user.mapper

import org.cityxplore.backend.user.dto.UserCreateRequest
import org.cityxplore.backend.user.dto.UserResponse
import org.cityxplore.backend.user.entity.User

fun User.toUserResponse(): UserResponse = UserResponse(
    id = this.id,
    email = this.email,
    username = this.username,
    avatarUrl = this.avatarUrl,
    createdAt = this.createdAt,
    lastActiveAt = this.lastActiveAt,
    totalDistance = this.totalDistance
)

fun List<User>.toUserResponseList(): List<UserResponse> = this.map { it.toUserResponse() }

fun UserCreateRequest.toEntity(id: java.util.UUID? = null): User = User(
    id = id,
    email = this.email,
    username = this.username,
    avatarUrl = this.avatarUrl
)
