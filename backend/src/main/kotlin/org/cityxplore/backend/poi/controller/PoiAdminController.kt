package org.cityxplore.backend.poi.controller

import jakarta.validation.Valid
import org.cityxplore.backend.poi.dto.CreatePoiDto
import org.cityxplore.backend.poi.dto.PointOfInterestDto
import org.cityxplore.backend.poi.service.PointOfInterestService
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

/**
 * Admin/debug controller for managing Points of Interest.
 *
 * Endpoints are secured for admins only (see SecurityConfig). Provides CRUD-like
 * operations using DTOs and consistent HTTP semantics.
 */
@RestController
@RequestMapping("/api/admin/pois")
class PoiAdminController(
    private val poiService: PointOfInterestService
) {

    @GetMapping
    fun getAll(): List<PointOfInterestDto> = poiService.getAllPois()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): PointOfInterestDto = poiService.getPoiById(id)

    @PostMapping
    fun createPoi(@Valid @RequestBody dto: CreatePoiDto): ResponseEntity<PointOfInterestDto> {
        val created = poiService.createPoi(dto)
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id).toUri()
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created)
    }

    @PutMapping("/{id}")
    fun updatePoi(@PathVariable id: UUID, @Valid @RequestBody dto: CreatePoiDto): ResponseEntity<PointOfInterestDto> {
        val updated = poiService.updatePoi(id, dto)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun deletePoi(@PathVariable id: UUID): ResponseEntity<Void> {
        poiService.deletePoi(id)
        return ResponseEntity.noContent().build()
    }
}
