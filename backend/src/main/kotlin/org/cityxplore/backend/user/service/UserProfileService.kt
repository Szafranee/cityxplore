package org.cityxplore.backend.user.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
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
    private val avatarBucket = "user-avatars" // Ensure this bucket exists in Supabase

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

    private fun validateImageFile(bytes: ByteArray) {
        if (bytes.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")
        if (bytes.size > 5 * 1024 * 1024) throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "File is too large (max 5MB)"
        )

        // Check magic bytes for JPEG, PNG, WebP
        // JPEG: FF D8 FF
        // PNG: 89 50 4E 47
        // WebP: RIFF ... WEBP
        val isJpeg =
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        val isPng =
            bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        val isWebp = bytes.size >= 12 &&
                bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()

        if (!isJpeg && !isPng && !isWebp) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid file format. Only JPG, PNG, and WebP are allowed."
            )
        }
    }

    private suspend fun deleteOldAvatar(oldUrl: String?) {
        if (oldUrl.isNullOrBlank()) return
        // Extract path from URL. Valid URLs:
        // https://.../storage/v1/object/public/user-avatars/USER_ID/FILENAME
        if (!oldUrl.contains("/$avatarBucket/")) return

        val path = oldUrl.substringAfter("/$avatarBucket/")
        if (path.isNotBlank()) {
            try {
                logger.info("Deleting old avatar: $path")
                val bucket = supabaseClient.storage.from(avatarBucket)
                bucket.delete(path)
            } catch (e: Exception) {
                logger.warn("Failed to delete old avatar: $oldUrl", e)
            }
        }
    }

    /**
     * Uploads a user avatar to Supabase Storage and updates the user profile.
     *
     * @param userId The ID of the user.
     * @param fileBytes The file content.
     * @param fileName The original filename.
     * @return The updated user profile.
     */
    @Transactional
    fun uploadUserAvatar(userId: UUID, fileBytes: ByteArray, fileName: String): UserProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (!user.isActive) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        validateImageFile(fileBytes)

        // Generate a unique path: {userId}/{timestamp}_{filename}
        val timestamp = System.currentTimeMillis()
        // Sanitise filename and ensure extension matches type loosely or force one?
        // We'll keep the original extension but sanitised.
        val safeFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val path = "$userId/${timestamp}_${safeFileName}"

        val oldAvatarUrl = user.avatarUrl

        val publicUrl = try {
            runBlocking {
                // Delete old if exists
                deleteOldAvatar(oldAvatarUrl)

                val bucket = supabaseClient.storage.from(avatarBucket)
                bucket.upload(path, fileBytes) {
                    upsert = true
                }
                bucket.publicUrl(path)
            }
        } catch (e: Exception) {
            logger.error("Failed to upload avatar to Supabase Storage", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar")
        }

        user.avatarUrl = publicUrl
        val saved = userRepository.save(user)
        return saved.toDto()
    }

    /**
     * Soft deletes a user account.
     *
     * Sets isActive to false, sets deletedAt to now, and anonymises sensitive data
     * (email, username, avatar) to allow re-registration (freeing unique constraints)
     * and follow privacy requirements.
     *
     * @param userId The unique identifier of the user to softly delete.
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
            // which prevents API access. Ideally, manual clean-up in Supabase might be needed if this fails.
            logger.error("Failed to delete user $userId from Supabase Auth. User is soft-deleted in DB.", e)
        }
    }
}
