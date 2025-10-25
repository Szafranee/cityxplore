package org.cityxplore.backend.achievements.controller

import jakarta.validation.Valid
import org.cityxplore.backend.achievements.dto.AchievementDto
import org.cityxplore.backend.achievements.entity.Achievement
import org.cityxplore.backend.achievements.repository.AchievementRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
 * Note: the request body may contain an `id`, but it is ignored for create and update
 * (the server generates/persists the id and returns it in the response).
 */
@RestController
@RequestMapping("/api/admin/achievements")
class AchievementAdminController(
    private val achievementRepository: AchievementRepository
) {

    /**
     * Creates a new achievement definition.
     * Returns 201 Created with Location header and the created dto.
     */
    @PostMapping
    fun create(@Valid @RequestBody achievementDto: AchievementDto): ResponseEntity<AchievementDto> {
        val entity = achievementRepository.save(
            Achievement(
                name = achievementDto.name,
                description = achievementDto.description,
                category = achievementDto.category,
                criteria = emptyMap(),
                iconUrl = achievementDto.iconUrl,
                points = achievementDto.points,
                isActive = true
            )
        )
        val body = achievementDto.copy(id = entity.id!!)
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(body.id).toUri()
        return ResponseEntity.created(location).body(body)
    }

    /**
     * Updates an existing achievement definition.
     * Returns 200 OK with updated representation or 404 if not found.
     */
    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody dto: AchievementDto): ResponseEntity<AchievementDto> {
        val existing = achievementRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found") }

        val updated = existing.copy(
            name = dto.name,
            description = dto.description,
            category = dto.category,
            iconUrl = dto.iconUrl,
            points = dto.points
        )
        val saved = achievementRepository.save(updated)
        return ResponseEntity.ok(dto.copy(id = saved.id!!))
    }

    /**
     * Lists available (distinct) achievement categories.
     */
    @GetMapping("/categories")
    fun getCategories(): List<String> =
        achievementRepository.findAll()
            .mapNotNull { it.category }
            .distinct()

    /**
     * Deletes an achievement definition by id. Returns 204 or 404 if not found.
     */
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        if (!achievementRepository.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found")
        }
        achievementRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
