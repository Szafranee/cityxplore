package org.cityxplore.backend.social.shared.mapper

import org.cityxplore.backend.social.shared.dto.CustomPoiData
import org.cityxplore.backend.social.shared.entity.SharedPoi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unit tests for SharedPoiMapper.
 */
class SharedPoiMapperTest {

    @Test
    fun `toResponse should correctly map SharedPoi with poiId and no custom data`() {
        // Given
        val id = UUID.randomUUID()
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val sharedAt = LocalDateTime.of(2025, 11, 8, 12, 30)
        val viewedAt = LocalDateTime.of(2025, 11, 8, 14, 0)
        val message = "Check this out!"

        val entity = SharedPoi(
            id = id,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = poiId,
            poiData = null,
            message = message,
            sharedAt = sharedAt,
            viewedAt = viewedAt
        )

        // When
        val response = SharedPoiMapper.toResponse(entity)

        // Then
        assertEquals(id, response.id)
        assertEquals(sharerId, response.sharerId)
        assertEquals(recipientId, response.recipientId)
        assertEquals(poiId, response.poiId)
        assertNull(response.poiData)
        assertEquals(message, response.message)
        assertEquals(sharedAt, response.sharedAt)
        assertEquals(viewedAt, response.viewedAt)
    }

    @Test
    fun `toResponse should correctly map SharedPoi with customPoi and no poiId`() {
        // Given
        val id = UUID.randomUUID()
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val sharedAt = LocalDateTime.of(2025, 11, 8, 10, 0)

        val customPoi = CustomPoiData(
            name = "Secret Spot",
            description = "A hidden gem",
            category = "Hidden Gems",
            latitude = 52.2297,
            longitude = 21.0122
        )

        val entity = SharedPoi(
            id = id,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = null,
            poiData = customPoi,
            message = "My favorite place!",
            sharedAt = sharedAt,
            viewedAt = null
        )

        // When
        val response = SharedPoiMapper.toResponse(entity)

        // Then
        assertEquals(id, response.id)
        assertEquals(sharerId, response.sharerId)
        assertEquals(recipientId, response.recipientId)
        assertNull(response.poiId)
        assertNotNull(response.poiData)
        assertEquals("Secret Spot", response.poiData?.name)
        assertEquals("A hidden gem", response.poiData?.description)
        assertEquals(52.2297, response.poiData?.latitude)
        assertEquals(21.0122, response.poiData?.longitude)
        assertEquals("My favorite place!", response.message)
        assertEquals(sharedAt, response.sharedAt)
        assertNull(response.viewedAt)
    }

    @Test
    fun `toResponse should correctly map SharedPoi with null message`() {
        // Given
        val id = UUID.randomUUID()
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val sharedAt = LocalDateTime.now()

        val entity = SharedPoi(
            id = id,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = poiId,
            poiData = null,
            message = null,
            sharedAt = sharedAt,
            viewedAt = null
        )

        // When
        val response = SharedPoiMapper.toResponse(entity)

        // Then
        assertEquals(id, response.id)
        assertEquals(sharerId, response.sharerId)
        assertEquals(recipientId, response.recipientId)
        assertEquals(poiId, response.poiId)
        assertNull(response.poiData)
        assertNull(response.message)
        assertEquals(sharedAt, response.sharedAt)
        assertNull(response.viewedAt)
    }

    @Test
    fun `toResponse should correctly map SharedPoi with null viewedAt (unviewed)`() {
        // Given
        val id = UUID.randomUUID()
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val sharedAt = LocalDateTime.now()

        val entity = SharedPoi(
            id = id,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = poiId,
            poiData = null,
            message = "Not viewed yet",
            sharedAt = sharedAt,
            viewedAt = null
        )

        // When
        val response = SharedPoiMapper.toResponse(entity)

        // Then
        assertNull(response.viewedAt)
        assertEquals("Not viewed yet", response.message)
    }

    @Test
    fun `toResponse should correctly map SharedPoi with empty customPoi fields`() {
        // Given
        val id = UUID.randomUUID()
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val sharedAt = LocalDateTime.now()

        val customPoi = CustomPoiData(
            name = "",
            description = "",
            category = "",
            latitude = 0.0,
            longitude = 0.0
        )

        val entity = SharedPoi(
            id = id,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = null,
            poiData = customPoi,
            message = null,
            sharedAt = sharedAt,
            viewedAt = null
        )

        // When
        val response = SharedPoiMapper.toResponse(entity)

        // Then
        assertNotNull(response.poiData)
        assertEquals("", response.poiData?.name)
        assertEquals("", response.poiData?.description)
        assertEquals(0.0, response.poiData?.latitude)
        assertEquals(0.0, response.poiData?.longitude)
    }

    @Test
    fun `toResponse should handle extreme coordinate values`() {
        // Given
        val id = UUID.randomUUID()
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val sharedAt = LocalDateTime.now()

        val customPoi = CustomPoiData(
            name = "North Pole",
            description = "Very north",
            category = "Landmarks",
            latitude = 90.0,
            longitude = 180.0
        )

        val entity = SharedPoi(
            id = id,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = null,
            poiData = customPoi,
            message = null,
            sharedAt = sharedAt,
            viewedAt = null
        )

        // When
        val response = SharedPoiMapper.toResponse(entity)

        // Then
        assertEquals(90.0, response.poiData?.latitude)
        assertEquals(180.0, response.poiData?.longitude)
    }

    @Test
    fun `toResponse should preserve all timestamps correctly`() {
        // Given
        val id = UUID.randomUUID()
        val sharerId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val poiId = UUID.randomUUID()

        val sharedAt = LocalDateTime.of(2025, 11, 7, 10, 30, 45)
        val viewedAt = LocalDateTime.of(2025, 11, 8, 15, 20, 10)

        val entity = SharedPoi(
            id = id,
            sharerId = sharerId,
            recipientId = recipientId,
            poiId = poiId,
            poiData = null,
            message = "Test",
            sharedAt = sharedAt,
            viewedAt = viewedAt
        )

        // When
        val response = SharedPoiMapper.toResponse(entity)

        // Then
        assertEquals(sharedAt, response.sharedAt)
        assertEquals(viewedAt, response.viewedAt)
        assertEquals(2025, response.sharedAt.year)
        assertEquals(11, response.sharedAt.monthValue)
        assertEquals(7, response.sharedAt.dayOfMonth)
        assertEquals(10, response.sharedAt.hour)
        assertEquals(30, response.sharedAt.minute)
        assertEquals(45, response.sharedAt.second)
    }
}
