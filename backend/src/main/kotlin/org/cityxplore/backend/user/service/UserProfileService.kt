package org.cityxplore.backend.user.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking
import org.cityxplore.backend.user.dto.UpdateUserProfileRequest
import org.cityxplore.backend.user.dto.UserProfileResponse
import org.cityxplore.backend.user.mapper.toDto
import org.cityxplore.backend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service class for managing user profiles, including operations such as retrieving or updating
 * a user's profile. This service interacts with the `UserRepository` to handle persistence and applies
 * business logic related to user profiles.
 *
 * @property userRepository Repository for accessing and manipulating user data in the database.
 * @property supabaseClient Supabase client for interacting with Auth service.
 */
@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val supabaseClient: SupabaseClient
) {

    private val logger = LoggerFactory.getLogger(UserProfileService::class.java)

    /**
     * Retrieves the profile information for a user based on their unique identifier.
     * If the user does not exist or is inactive (soft-deleted), a `ResponseStatusException` is thrown with a 404 status.
     *
     * @param userId The unique identifier of the user whose profile is to be retrieved.
     * @return A `UserProfileResponse` containing the profile details of the specified user.
     */
    @Transactional(readOnly = true)
    fun getUserProfile(userId: UUID): UserProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (!user.isActive) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        return user.toDto()
    }

    /**
     * Updates the profile of an existing user based on the provided data.
     *
     * This method allows modifying specific fields of a user's profile, such as their
     * username and avatar URL. If the user does not exist or is inactive, a `ResponseStatusException` is thrown
     * with a 404 Not Found status.
     *
     * @param userId The unique identifier of the user whose profile is to be updated.
     * @param patch An instance of `UpdateUserProfileRequest` containing the new profile details to update.
     *              Fields that are null in the DTO will not be updated.
     * @return An updated `UserProfileResponse` representing the user's profile after the changes
     */
    @Transactional
    fun updateUserProfile(userId: UUID, patch: UpdateUserProfileRequest): UserProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (!user.isActive) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        patch.username?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { user.username = it }
        patch.avatarUrl?.let { user.avatarUrl = it }

        val saved = try {
            userRepository.save(user)
        } catch (e: DataIntegrityViolationException) {
            val message = if (e.message?.contains("username", ignoreCase = true) == true) {
                "Username already taken"
            } else {
                "Constraint violation occurred"
            }
            throw ResponseStatusException(HttpStatus.CONFLICT, message)
        }

        return saved.toDto()
    }

    /**
     * Soft deletes a user account.
     *
     * Sets isActive to false, sets deletedAt to now, and anonymizes sensitive data
     * (email, username, avatar) to allow re-registration (freeing unique constraints)
     * and follow privacy requirements.
     *
     * @param userId The unique identifier of the user to soft delete.
     */
    @Transactional
    fun deleteUserAccount(userId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (user.deletedAt != null || !user.isActive) {
            return // Already deleted
        }

        val timestamp = System.currentTimeMillis()

        user.isActive = false
        user.deletedAt = LocalDateTime.now()

        // 1. Free up Email constraint: use sub-addressing (alias) to keep it valid but unique-different.
        // Original: user@example.com -> user+deleted123456@example.com
        // This allows the user to register again with "user@example.com".
        user.email = user.email.replace("@", "+deleted$timestamp@")

        // 2. Free up Username constraint: Append suffix, ensuring we don't exceed max length (50 chars).
        val usernameSuffix = "_del_$timestamp"
        val maxPrefixLen = (50 - usernameSuffix.length).coerceAtLeast(0)
        user.username = user.username.take(maxPrefixLen) + usernameSuffix

        // 3. Remove PII
        user.avatarUrl = null

        userRepository.save(user)

        // 4. Delete user from Supabase Auth (Invalidate tokens and block login)
        try {
            logger.info("Deactivating user $userId: Soft delete in DB completed. Removing from Supabase Auth using SDK...")

            // Blocking call for Supabase SDK since Spring MVC is blocking by default.
            runBlocking {
                supabaseClient.auth.admin.deleteUser(userId.toString())
            }

            logger.info("User $userId successfully removed from Supabase Auth.")

        } catch (e: Exception) {
            // We log the error but do not fail the transaction. The user is soft-deleted in our DB,
            // which prevents API access. Ideally, manual cleanup in Supabase might be needed if this fails.
            logger.error("Failed to delete user $userId from Supabase Auth. User is soft-deleted in DB.", e)
        }
    }
}
