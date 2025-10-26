package org.cityxplore.backend.achievements.controller

import jakarta.validation.Valid
import org.cityxplore.backend.achievements.dto.AchievementResponse
import org.cityxplore.backend.achievements.dto.CreateAchievementRequest
import org.cityxplore.backend.achievements.dto.UpdateAchievementRequest
import org.cityxplore.backend.achievements.mapper.applyTo
import org.cityxplore.backend.achievements.mapper.toDto
import org.cityxplore.backend.achievements.mapper.toEntity
import org.cityxplore.backend.achievements.repository.AchievementRepository
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

/**
 * Admin controller for managing achievement definitions.
 *
 * Security: endpoints are protected via hasRole("ADMIN"). See SecurityConfig.
 * Input DTOs are separated from output DTOs to keep validation concerns clear.
 */
@RestController
@RequestMapping("/api/admin/achievements")
@PreAuthorize("hasRole('ADMIN')")
class AchievementAdminController(
    private val achievementRepository: AchievementRepository
) {

    /**
     * Creates a new achievement definition.
     * Returns 201 Created with Location header and the created dto.
     */
    @PostMapping
    fun create(@Valid @RequestBody createAchievement: CreateAchievementRequest): ResponseEntity<AchievementResponse> {
        val saved = achievementRepository.save(createAchievement.toEntity())
        val body = saved.toDto()
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(body.id).toUri()

        return ResponseEntity.created(location).body(body)
    }

    /**
     * Updates an existing achievement definition.
     * Returns 200 OK with updated representation or 404 if not found.
     */
    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody updateAchievement: UpdateAchievementRequest
    ): ResponseEntity<AchievementResponse> {
        val existing = achievementRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found") }

        val saved = achievementRepository.save(updateAchievement.applyTo(existing))
        val body = saved.toDto()

        return ResponseEntity.ok(body)
    }

    /**
     * Lists available (distinct) achievement categories.
     */
    @GetMapping("/categories")
    fun getCategories(): List<String> = achievementRepository.findDistinctCategories()

    /**
     * Deletes an achievement definition by id. Returns 204 or 404 if not found.
     */
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        return try {
            achievementRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } catch (_: EmptyResultDataAccessException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found")
        }
    }
}
