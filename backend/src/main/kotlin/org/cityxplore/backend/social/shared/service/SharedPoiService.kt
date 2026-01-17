package org.cityxplore.backend.social.shared.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import org.cityxplore.backend.poi.repository.PointOfInterestRepository
import org.cityxplore.backend.social.friendship.repository.FriendshipRepository
import org.cityxplore.backend.social.shared.dto.SharePoiRequest
import org.cityxplore.backend.social.shared.dto.SharedPoiResponse
import org.cityxplore.backend.social.shared.entity.SharedPoi
import org.cityxplore.backend.social.shared.mapper.SharedPoiMapper
import org.cityxplore.backend.social.shared.repository.SharedPoiRepository
import org.cityxplore.backend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service responsible for managing shared Points of Interest between users.
 * Handles sharing POIs, tracking views, and retrieving shared POI history.
 * POIs can only be shared between users who are accepted friends.
 */
@Service
class SharedPoiService(
    private val sharedPoiRepository: SharedPoiRepository,
    private val poiRepository: PointOfInterestRepository,
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val supabaseClient: SupabaseClient
) {
    private val logger = LoggerFactory.getLogger(SharedPoiService::class.java)
    private val poiImagesBucket = "poi-images" // Ensure this bucket exists in Supabase (public)

    /**
     * Uploads an image for a custom POI to Supabase Storage.
     *
     * @param userId The ID of the uploading user.
     * @param file The multipart file containing the image.
     * @return The public URL of the uploaded image.
     */
    fun uploadPoiImage(userId: UUID, file: MultipartFile): String {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty")
        }

        val fileBytes = file.bytes
        val contentType = detectMimeType(fileBytes) ?: run {
            logger.warn("Could not detect MIME type for uploaded file")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file format")
        }

        if (!listOf(ContentType.Image.JPEG, ContentType.Image.PNG, ContentType.Image.WEBP).contains(contentType)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG and WEBP images are supported")
        }

        val fileExtension = contentType.contentSubtype
        val filename = "${UUID.randomUUID()}.$fileExtension"
        val path = "$userId/$filename"

        return try {
            val bucket = supabaseClient.storage.from(poiImagesBucket)
            runBlocking {
                bucket.upload(path, fileBytes) {
                    upsert = false
                }
            }
            bucket.publicUrl(path)
        } catch (e: Exception) {
            logger.error("Failed to upload POI image to Supabase Storage", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image")
        }
    }

    private fun detectMimeType(bytes: ByteArray): ContentType? {
        // Simple magic bytes check
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return ContentType.Image.PNG
        }
        // JPEG: FF D8 FF
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        ) {
            return ContentType.Image.JPEG
        }
        // WEBP: RIFF ... WEBP
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()
        ) {
            return ContentType.Image.WEBP
        }
        return null
    }

    /**
     * Shares a Point of Interest with another user.
     *
     * This method validates that:
     * - Either poiId OR customPoi is provided (XOR logic)
     * - The sharer is not sharing with themselves
     * - The recipient user exists
     * - The users are accepted friends
     * - If poiId is provided, the POI exists in the database
     *
     * @param sharerId the UUID of the user sharing the POI
     * @param sharePoiRequest the request containing recipient ID, either poiId or customPoi, and optional message
     * @return a SharedPoiResponse representing the created shared POI record
     * @throws ResponseStatusException if validation fails (400 BAD_REQUEST, 403 FORBIDDEN, or 404 NOT_FOUND)
     */
    @Transactional
    fun sharePoi(sharerId: UUID, sharePoiRequest: SharePoiRequest): SharedPoiResponse {
        // Validate XOR: exactly one of poiId or customPoi must be provided
        val hasPoiId = sharePoiRequest.poiId != null
        val hasCustomPoi = sharePoiRequest.customPoi != null

        if (hasPoiId == hasCustomPoi) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Exactly one of poiId or customPoi must be provided"
            )
        }

        if (sharerId == sharePoiRequest.recipientId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot share with yourself")
        }

        if (!userRepository.existsById(sharePoiRequest.recipientId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found")
        }

        if (!friendshipRepository.areFriends(sharerId, sharePoiRequest.recipientId)) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You can only share POIs with accepted friends"
            )
        }

        // If sharing an existing POI, validate it exists
        if (hasPoiId && !poiRepository.existsById(sharePoiRequest.poiId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")
        }

        val shared = sharedPoiRepository.save(
            SharedPoi(
                sharerId = sharerId,
                recipientId = sharePoiRequest.recipientId,
                poiId = sharePoiRequest.poiId,
                poiData = sharePoiRequest.customPoi,
                message = sharePoiRequest.message
            )
        )

        val sharer = userRepository.findById(sharerId).orElse(null)
        val recipient = userRepository.findById(sharePoiRequest.recipientId).orElse(null)

        return SharedPoiMapper.toResponse(shared, sharer, recipient)
    }

    /**
     * Retrieves details of a specific shared POI by its ID.
     *
     * This method ensures that the requesting user is either the sharer or recipient
     * of the shared POI record.
     *
     * @param currentUserId the UUID of the user requesting the shared POI details
     * @param sharedPoiId the unique identifier of the shared POI to retrieve
     * @return a SharedPoiResponse containing the shared POI details
     * @throws ResponseStatusException if the shared POI does not exist or the user has no access
     */
    @Transactional(readOnly = true)
    fun getSharedPoiById(currentUserId: UUID, sharedPoiId: UUID): SharedPoiResponse {
        val sharedPoi = sharedPoiRepository.findById(sharedPoiId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Shared POI not found") }

        if (sharedPoi.sharerId != currentUserId && sharedPoi.recipientId != currentUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this shared POI")
        }

        val sharer = userRepository.findById(sharedPoi.sharerId).orElse(null)
        val recipient = userRepository.findById(sharedPoi.recipientId).orElse(null)

        return SharedPoiMapper.toResponse(sharedPoi, sharer, recipient)
    }

    /**
     * Retrieves all POIs shared by a specific user.
     *
     * @param userId the UUID of the user whose shared POIs to retrieve
     * @return list of SharedPoiResponse objects representing POIs shared by the user
     */
    @Transactional(readOnly = true)
    fun getSharedByMe(userId: UUID): List<SharedPoiResponse> {
        val sharedPois = sharedPoiRepository.findAllBySharerId(userId)
        val sharer = userRepository.findById(userId).orElse(null)

        // Get all unique recipient IDs and fetch their data
        val recipientIds = sharedPois.map { it.recipientId }.toSet()
        val recipients = userRepository.findAllById(recipientIds).associateBy { it.id!! }

        return sharedPois.map { sharedPoi ->
            SharedPoiMapper.toResponse(
                sharedPoi = sharedPoi,
                sharer = sharer,
                recipient = recipients[sharedPoi.recipientId]
            )
        }
    }

    /**
     * Retrieves all POIs shared to a specific user.
     *
     * @param userId the UUID of the user who received shared POIs to retrieve
     * @return list of SharedPoiResponse objects representing POIs shared to the user
     */
    @Transactional(readOnly = true)
    fun getSharedToMe(userId: UUID): List<SharedPoiResponse> {
        val sharedPois = sharedPoiRepository.findAllByRecipientId(userId)
        val recipient = userRepository.findById(userId).orElse(null)

        // Get all unique sharer IDs and fetch their data
        val sharerIds = sharedPois.map { it.sharerId }.toSet()
        val sharers = userRepository.findAllById(sharerIds).associateBy { it.id!! }

        return sharedPois.map { sharedPoi ->
            SharedPoiMapper.toResponse(
                sharedPoi = sharedPoi,
                sharer = sharers[sharedPoi.sharerId],
                recipient = recipient
            )
        }
    }

    /**
     * Retrieves all unviewed POIs shared to a specific user.
     *
     * @param userId the UUID of the user whose unviewed shared POIs to retrieve
     * @return list of SharedPoiResponse objects representing unviewed POIs shared to the user
     */
    @Transactional(readOnly = true)
    fun getUnviewedSharedToMe(userId: UUID): List<SharedPoiResponse> {
        val sharedPois = sharedPoiRepository.findAllByRecipientIdAndViewedAtIsNull(userId)
        val recipient = userRepository.findById(userId).orElse(null)

        // Get all unique sharer IDs and fetch their data
        val sharerIds = sharedPois.map { it.sharerId }.toSet()
        val sharers = userRepository.findAllById(sharerIds).associateBy { it.id!! }

        return sharedPois.map { sharedPoi ->
            SharedPoiMapper.toResponse(
                sharedPoi = sharedPoi,
                sharer = sharers[sharedPoi.sharerId],
                recipient = recipient
            )
        }
    }

    /**
     * Marks a shared POI as viewed by the recipient.
     *
     * This method validates that the current user is the intended recipient
     * before updating the viewed timestamp.
     *
     * @param recipientId the UUID of the user marking the POI as viewed
     * @param sharedId the UUID of the shared POI record to mark as viewed
     * @return a SharedPoiResponse representing the updated shared POI record
     * @throws ResponseStatusException if the shared POI does not exist or user has no access
     */
    @Transactional
    fun markViewed(recipientId: UUID, sharedId: UUID): SharedPoiResponse {
        val shared = sharedPoiRepository.findById(sharedId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Shared POI not found") }

        if (shared.recipientId != recipientId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your shared POI")
        }

        shared.viewedAt = LocalDateTime.now()
        val updated = sharedPoiRepository.save(shared)

        val sharer = userRepository.findById(updated.sharerId).orElse(null)
        val recipient = userRepository.findById(updated.recipientId).orElse(null)

        return SharedPoiMapper.toResponse(updated, sharer, recipient)
    }

    /**
     * Deletes a shared POI record.
     *
     * This method allows the sharer to remove a POI they have shared with another user.
     * Only the user who originally shared the POI can delete it.
     *
     * @param sharerId the UUID of the user who shared the POI
     * @param sharedPoiId the UUID of the shared POI record to delete
     * @throws ResponseStatusException if the shared POI does not exist or the user is not the sharer
     */
    @Transactional
    fun deleteSharedPoi(sharerId: UUID, sharedPoiId: UUID) {
        val sharedPoi = sharedPoiRepository.findById(sharedPoiId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Shared POI not found") }

        if (sharedPoi.sharerId != sharerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete POIs that you have shared")
        }

        sharedPoiRepository.delete(sharedPoi)
    }
}
