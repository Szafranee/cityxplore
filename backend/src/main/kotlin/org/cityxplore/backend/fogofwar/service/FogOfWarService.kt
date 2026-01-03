package org.cityxplore.backend.fogofwar.service

import jakarta.annotation.PostConstruct
import org.cityxplore.backend.fogofwar.entity.FogOfWarEntity
import org.cityxplore.backend.fogofwar.generator.WarsawHexagonGenerator
import org.cityxplore.backend.fogofwar.model.FogOfWarResponse
import org.cityxplore.backend.fogofwar.model.RevealHexagonsResponse
import org.cityxplore.backend.fogofwar.model.WarsawBounds
import org.cityxplore.backend.fogofwar.model.WarsawHexagonsResponse
import org.cityxplore.backend.fogofwar.repository.FogOfWarRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Service layer for Fog of War feature.
 *
 * Handles:
 * - Generation and caching of Warsaw hexagons
 * - User fog of war progress (CRUD operations)
 * - Set union operations for hexagon reveals
 */
@Service
class FogOfWarService(
    private val hexagonGenerator: WarsawHexagonGenerator,
    private val repository: FogOfWarRepository
) {

    private val logger = LoggerFactory.getLogger(FogOfWarService::class.java)
    private lateinit var warsawHexagonsCache: WarsawHexagonsResponse

    /**
     * Initialises the service by generating Warsaw hexagons on startup.
     * Hexagons are generated once and cached in memory for performance.
     */
    @PostConstruct
    fun init() {
        logger.info("Initializing Fog of War service...")

        val hexagons = hexagonGenerator.generateHexagons(resolution = 10)

        warsawHexagonsCache = WarsawHexagonsResponse(
            resolution = 10,
            hexagons = hexagons,
            totalCount = hexagons.size,
            bounds = WarsawBounds(),
            generatedAt = Instant.now()
        )

        logger.info("Fog of War initialized with ${hexagons.size} Warsaw hexagons")
    }

    /**
     * Returns pre-computed Warsaw hexagons.
     * This response is cached in memory and identical for all users.
     *
     * @return WarsawHexagonsResponse containing all hexagons
     */
    fun getWarsawHexagons(): WarsawHexagonsResponse = warsawHexagonsCache

    /**
     * Retrieves user's revealed hexagons.
     *
     * @param userId User's unique identifier
     * @return FogOfWarResponse with user's revealed hexagons
     */
    fun getUserRevealedHexagons(userId: UUID): FogOfWarResponse {
        return repository.findByUserId(userId)
            ?.let { entity ->
                FogOfWarResponse(
                    userId = entity.userId.toString(),
                    revealedHexagons = entity.revealedHexagons.toSet(),
                    lastUpdated = entity.updatedAt
                )
            }
            ?: FogOfWarResponse(
                userId = userId.toString(),
                revealedHexagons = emptySet(),
                lastUpdated = null
            )
    }

    /**
     * Reveals new hexagons for a user.
     * Uses set union to prevent duplicates and ensure idempotency.
     *
     * @param userId User's unique identifier
     * @param hexagons Set of hexagon indices to reveal
     * @return RevealHexagonsResponse with operation result
     */
    @Transactional
    fun revealHexagons(userId: UUID, hexagons: Set<String>): RevealHexagonsResponse {
        val existing = repository.findByUserId(userId)

        val updated = if (existing != null) {
            // Set union: add new hexagons to existing
            existing.revealedHexagons.addAll(hexagons)
            repository.save(existing)
        } else {
            // Create new record
            repository.save(
                FogOfWarEntity(
                    userId = userId,
                    revealedHexagons = hexagons.toMutableSet()
                )
            )
        }

        return RevealHexagonsResponse(
            message = "Successfully revealed ${hexagons.size} hexagons",
            totalRevealed = updated.revealedHexagons.size
        )
    }

    /**
     * Clears all revealed hexagons for a user.
     * Used primarily for testing and debugging.
     *
     * @param userId User's unique identifier
     */
    @Transactional
    fun clearAllRevealed(userId: UUID) {
        repository.deleteByUserId(userId)
        logger.info("Cleared all fog of war data for user $userId")
    }
}
