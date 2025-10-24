package org.cityxplore.backend.service

import org.cityxplore.backend.dto.UpdateUserProfileDto
import org.cityxplore.backend.dto.UserProfileDto
import org.cityxplore.backend.entity.User
import org.cityxplore.backend.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Service class for managing user profiles, including operations such as retrieving or updating
 * a user's profile. This service interacts with the `UserRepository` to handle persistence and applies
 * business logic related to user profiles.
 *
 * @property userRepository Repository for accessing and manipulating user data in the database.
 */
@Service
class UserProfileService(
    private val userRepository: UserRepository
) {

    /**
     * Retrieves the profile information for a user based on their unique identifier.
     * If the user does not exist, a `ResponseStatusException` is thrown with a 404 status.
     *
     * @param userId The unique identifier of the user whose profile is to be retrieved.
     * @return A `UserProfileDto` containing the profile details of the specified user.
     */
    @Transactional(readOnly = true)
    fun getUserProfile(userId: UUID): UserProfileDto {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        return user.toDto()
    }

    /**
     * Updates the profile of an existing user based on the provided data.
     *
     * This method allows modifying specific fields of a user's profile, such as their
     * username and avatar URL. If the user does not exist, a `ResponseStatusException` is thrown
     * with a 404 Not Found status.
     *
     * @param userId The unique identifier of the user whose profile is to be updated.
     * @param patch An instance of `UpdateUserProfileDto` containing the new profile details to update.
     *              Fields that are null in the DTO will not be updated.
     * @return An updated `UserProfileDto` representing the user's profile after the changes*/
    @Transactional
    fun updateUserProfile(userId: UUID, patch: UpdateUserProfileDto): UserProfileDto {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        patch.username?.let { user.username = it }
        patch.avatarUrl?.let { user.avatarUrl = it }

        val saved = userRepository.save(user)
        return saved.toDto()
    }
}

/**
 * Converts a `User` entity to a `UserProfileDto`.
 *
 * This extension function maps the `User` entity's properties to the corresponding fields
 * in the `UserProfileDto` data transfer object. It is typically used for returning
 * user profile information in a format suitable for external consumers, such as APIs.
 *
 * @receiver The `User` entity instance to be converted.
 * @return A `UserProfileDto` object containing the mapped properties from the `User` entity.
 */
private fun User.toDto() = UserProfileDto(
    id = id!!,
    email = email,
    username = username,
    avatarUrl = avatarUrl,
    totalDistance = totalDistance,
    totalPoisDiscovered = totalPoisDiscovered,
    createdAt = createdAt
)
