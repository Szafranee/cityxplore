package org.cityxplore.backend.user.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.runBlocking
import org.apache.tika.Tika
import org.cityxplore.backend.discoveries.repository.UserPoiDiscoveryRepository
import org.cityxplore.backend.shared.config.GamificationConfig
import org.cityxplore.backend.user.dto.UpdateUserProfileRequest
import org.cityxplore.backend.user.dto.UserProfileResponse
import org.cityxplore.backend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Service class for managing user profiles, including operations such as retrieving or updating
 * a user's profile. This service interacts with the `UserRepository` to handle persistence and applies
 * business logic related to user profiles.
 *
 * @property userRepository Repository for accessing and manipulating user data in the database.
 * @property userPoiDiscoveryRepository Repository for counting user's POI discoveries.
 * @property supabaseClient Supabase client for interacting with Auth service.
 * @property gamificationConfig Configuration for XP points awarded per activity.
 */
@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val userPoiDiscoveryRepository: UserPoiDiscoveryRepository,
    private val supabaseClient: SupabaseClient,
    private val transactionTemplate: TransactionTemplate,
    private val gamificationConfig: GamificationConfig
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

        // Calculate total POIs discovered dynamically from user_poi_discoveries table
        val totalPoisDiscovered = userPoiDiscoveryRepository.countByUserId(userId).toInt()

        return UserProfileResponse(
            id = user.id!!,
            email = user.email,
            username = user.username,
            avatarUrl = user.avatarUrl,
            totalDistance = user.totalDistance,
            totalPoisDiscovered = totalPoisDiscovered,
            totalAchievementPoints = user.totalAchievementPoints,
            createdAt = user.createdAt
        )
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

        // Handle avatar URL update (e.g. predefined avatars)
        // If avatar changes, try to clean up the old one if it was ours
        patch.avatarUrl?.let { newUrl ->
            if (newUrl != user.avatarUrl) {
                val oldUrl = user.avatarUrl
                user.avatarUrl = newUrl

                // Register post-commit hook to delete old avatar
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                        override fun afterCommit() {
                            runBlocking { deleteOldAvatar(oldUrl) }
                        }
                    })
                }
            }
        }

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

        // Calculate total POIs discovered dynamically
        val totalPoisDiscovered = userPoiDiscoveryRepository.countByUserId(userId).toInt()

        return UserProfileResponse(
            id = saved.id!!,
            email = saved.email,
            username = saved.username,
            avatarUrl = saved.avatarUrl,
            totalDistance = saved.totalDistance,
            totalPoisDiscovered = totalPoisDiscovered,
            totalAchievementPoints = saved.totalAchievementPoints,
            createdAt = saved.createdAt
        )
    }

    /**
     * Deletes the old avatar file from Supabase Storage.
     *
     * @param oldUrl the URL/path of the avatar to delete
     */
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
     * Compensating action to delete a newly uploaded avatar if the DB save fails.
     *
     * @param url the URL of the uploaded avatar
     */
    private fun deleteUploadedAvatar(url: String) {
        val path = url.substringAfter("/$avatarBucket/")
        if (path.isNotBlank() && url.contains("/$avatarBucket/")) {
            runBlocking {
                try {
                    val bucket = supabaseClient.storage.from(avatarBucket)
                    bucket.delete(path)
                } catch (e: Exception) {
                    logger.warn("Failed to delete optimistically uploaded avatar: $url", e)
                }
            }
        }
    }

    /**
     * Uploads a user avatar to Supabase Storage and updates the user profile.
     *
     * @param userId The ID of the user.
     * @param file The multipart file containing the avatar image.
     * @return The updated user profile.
     */
    // Removed @Transactional from here to handle IO outside DB transaction
    fun uploadUserAvatar(userId: UUID, file: MultipartFile): UserProfileResponse {
        val exists = userRepository.existsById(userId)
        if (!exists) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        validateAvatarFile(file)

        val fileName = file.originalFilename ?: "avatar"
        val fileBytes = file.bytes

        // Generate a unique path: {userId}/{timestamp}_{filename}
        val timestamp = System.currentTimeMillis()
        val safeFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val path = "$userId/${timestamp}_${safeFileName}"

        val publicUrl = try {
            runBlocking {
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

        return try {
            saveUserAvatar(userId, publicUrl)
        } catch (e: Exception) {
            // Compensating transaction: delete uploaded file
            deleteUploadedAvatar(publicUrl)
            throw e
        }
    }

    private fun validateAvatarFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")
        }

        val contentType = file.contentType
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid content type. Only images are allowed.")
        }

        val maxSizeBytes = 5 * 1024 * 1024
        if (file.size > maxSizeBytes) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "File is too large. Max size is ${maxSizeBytes / (1024 * 1024)}MB."
            )
        }

        val detectedType = try {
            Tika().detect(file.inputStream)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to detect file type")
        }

        if (!detectedType.startsWith("image/")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not a valid image.")
        }
    }

    protected fun saveUserAvatar(userId: UUID, publicUrl: String): UserProfileResponse {
        return transactionTemplate.execute { _ ->
            val user = userRepository.findById(userId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

            if (!user.isActive) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            }

            val oldAvatarUrl = user.avatarUrl
            user.avatarUrl = publicUrl
            val saved = userRepository.save(user)

            // Register cleanup for old avatar
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    runBlocking { deleteOldAvatar(oldAvatarUrl) }
                }
            })

            // Calculate total POIs discovered dynamically
            val totalPoisDiscovered = userPoiDiscoveryRepository.countByUserId(userId).toInt()

            UserProfileResponse(
                id = saved.id!!,
                email = saved.email,
                username = saved.username,
                avatarUrl = saved.avatarUrl,
                totalDistance = saved.totalDistance,
                totalPoisDiscovered = totalPoisDiscovered,
                totalAchievementPoints = saved.totalAchievementPoints,
                createdAt = saved.createdAt
            )
        }!!
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
        // Use Instant or OffsetDateTime
        user.deletedAt = OffsetDateTime.now(ZoneOffset.UTC)
            .toLocalDateTime()

        // 1. Free up Email constraint and avoid PII
        // Generate anonymised address: deleted-{shortUUID}-{timestamp}@...
        // Max length 255.
        val shortUuid = UUID.randomUUID().toString().substring(0, 8)
        val anonEmailLocal = "deleted-$shortUuid-$timestamp"
        val anonDomain = "deleted.cityxplore.app"
        val newEmail = "$anonEmailLocal@$anonDomain"

        user.email = if (newEmail.length > 255) newEmail.take(255) else newEmail

        // 2. Free up Username constraint and avoid PII
        // "deleted-{shortId}" string truncated to username max (50).
        val anonUsernameBase = "deleted-$shortUuid"
        user.username = if (anonUsernameBase.length > 50) anonUsernameBase.take(50) else anonUsernameBase

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
            logger.error("Failed to delete user $userId from Supabase Auth. User is soft-deleted in DB.", e)
        }
    }

    /**
     * Adds travelled distance to a user's total distance and awards XP points.
     *
     * This method increments the user's totalDistance counter and awards XP points
     * based on the distance (configured in gamificationConfig).
     * The distance should be validated before calling this method (max 500m per request for anti-cheat).
     *
     * @param userId The unique identifier of the user.
     * @param distanceMeters The distance to add in meters.
     * @return The updated user profile.
     * @throws ResponseStatusException if the user is not found.
     */
    @Transactional
    fun addDistance(userId: UUID, distanceMeters: Double): UserProfileResponse {
        val distance = BigDecimal.valueOf(distanceMeters).setScale(2, RoundingMode.HALF_UP)

        val updated = userRepository.incrementDistance(userId, distance)
        if (updated == 0) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        // Award XP points for distance (1 point per 100 meters)
        val pointsPer100m = gamificationConfig.pointsPer100Meters
        val pointsToAward = (distanceMeters / 100.0 * pointsPer100m).toInt()
        if (pointsToAward > 0) {
            userRepository.incrementAchievementPoints(userId, pointsToAward)
            logger.debug("Awarded {} XP to user {} for {} meters traveled", pointsToAward, userId, distanceMeters)
        }

        logger.debug("Added {} meters to user {} total distance", distance, userId)
        return getUserProfile(userId)
    }
}
