package org.cityxplore.backend.discoveries.mapper

import org.cityxplore.backend.discoveries.entity.UserPoiDiscovery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unit tests for UserPoiDiscoveryMapper extension functions.
 */
class UserPoiDiscoveryMapperTest {

    @Test
    fun `toDto should map UserPoiDiscovery to UserPoiDiscoveryResponse correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discoveredAt = LocalDateTime.now()
        val discovery = UserPoiDiscovery(
            userId = userId,
            poiId = poiId,
            discoveredAt = discoveredAt,
            isFavorite = true
        )

        // When
        val response = discovery.toDto()

        // Then
        assertEquals(poiId, response.poiId)
        assertEquals(discoveredAt, response.discoveredAt)
        assertTrue(response.favorite)
    }

    @Test
    fun `toDto should map non-favorite discovery correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discoveredAt = LocalDateTime.now()
        val discovery = UserPoiDiscovery(
            userId = userId,
            poiId = poiId,
            discoveredAt = discoveredAt,
            isFavorite = false
        )

        // When
        val response = discovery.toDto()

        // Then
        assertEquals(poiId, response.poiId)
        assertEquals(discoveredAt, response.discoveredAt)
        assertFalse(response.favorite)
    }

    @Test
    fun `toDtoList should map empty list correctly`() {
        // Given
        val emptyList = emptyList<UserPoiDiscovery>()

        // When
        val result = emptyList.toDtoList()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toDtoList should map list with single element correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId = UUID.randomUUID()
        val discoveredAt = LocalDateTime.now()
        val discovery = UserPoiDiscovery(
            userId = userId,
            poiId = poiId,
            discoveredAt = discoveredAt,
            isFavorite = true
        )
        val list = listOf(discovery)

        // When
        val result = list.toDtoList()

        // Then
        assertEquals(1, result.size)
        assertEquals(poiId, result[0].poiId)
        assertEquals(discoveredAt, result[0].discoveredAt)
        assertTrue(result[0].favorite)
    }

    @Test
    fun `toDtoList should map list with multiple elements correctly`() {
        // Given
        val userId = UUID.randomUUID()
        val poiId1 = UUID.randomUUID()
        val poiId2 = UUID.randomUUID()
        val poiId3 = UUID.randomUUID()
        val discoveredAt1 = LocalDateTime.now()
        val discoveredAt2 = LocalDateTime.now().minusDays(1)
        val discoveredAt3 = LocalDateTime.now().minusDays(2)

        val discovery1 = UserPoiDiscovery(
            userId = userId,
            poiId = poiId1,
            discoveredAt = discoveredAt1,
            isFavorite = true
        )
        val discovery2 = UserPoiDiscovery(
            userId = userId,
            poiId = poiId2,
            discoveredAt = discoveredAt2,
            isFavorite = false
        )
        val discovery3 = UserPoiDiscovery(
            userId = userId,
            poiId = poiId3,
            discoveredAt = discoveredAt3,
            isFavorite = true
        )
        val list = listOf(discovery1, discovery2, discovery3)

        // When
        val result = list.toDtoList()

        // Then
        assertEquals(3, result.size)

        // Verify first element
        assertEquals(poiId1, result[0].poiId)
        assertEquals(discoveredAt1, result[0].discoveredAt)
        assertTrue(result[0].favorite)

        // Verify second element
        assertEquals(poiId2, result[1].poiId)
        assertEquals(discoveredAt2, result[1].discoveredAt)
        assertFalse(result[1].favorite)

        // Verify third element
        assertEquals(poiId3, result[2].poiId)
        assertEquals(discoveredAt3, result[2].discoveredAt)
        assertTrue(result[2].favorite)
    }

    @Test
    fun `toDtoList should preserve order of elements`() {
        // Given
        val userId = UUID.randomUUID()
        val discoveries = (1..5).map {
            UserPoiDiscovery(
                userId = userId,
                poiId = UUID.randomUUID(),
                discoveredAt = LocalDateTime.now().minusDays(it.toLong()),
                isFavorite = it % 2 == 0
            )
        }

        // When
        val result = discoveries.toDtoList()

        // Then
        assertEquals(5, result.size)
        discoveries.forEachIndexed { index, discovery ->
            assertEquals(discovery.poiId, result[index].poiId)
            assertEquals(discovery.discoveredAt, result[index].discoveredAt)
            assertEquals(discovery.isFavorite, result[index].favorite)
        }
    }
}
