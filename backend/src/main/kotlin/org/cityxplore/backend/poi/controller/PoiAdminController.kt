package org.cityxplore.backend.poi.controller

import jakarta.validation.Valid
import org.cityxplore.backend.poi.dto.CreatePoiRequest
import org.cityxplore.backend.poi.dto.PoiAdminResponse
import org.cityxplore.backend.poi.dto.UpdatePoiRequest
import org.cityxplore.backend.poi.service.PointOfInterestService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
@PreAuthorize("hasRole('ADMIN')")
class PoiAdminController(
    private val pointOfInterestService: PointOfInterestService
) {

    /**
     * Retrieves all Points of Interest (POIs) and returns them as a list of admin-facing response objects.
     *
     * @return A list of `PoiAdminResponse` objects representing all Points of Interest available for administration.
     */
    @GetMapping
    fun getAll(): List<PoiAdminResponse> = pointOfInterestService.getAllPois()

    /**
     * Retrieves a specific Point of Interest (POI) by its unique identifier.
     *
     * @param id The unique identifier of the POI to retrieve.
     * @return A `PoiAdminResponse` object representing the retrieved Point of Interest.
     */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): PoiAdminResponse = pointOfInterestService.getPoiById(id)

    /**
     * Handles HTTP POST requests to create a new Point of Interest (POI). The POI is created with
     * details provided in the request body and stored in the underlying repository. Returns details
     * of the created POI along with the resource location in the response headers.
     *
     * @param createPoi The request object containing details for the new POI. Includes fields like name,
     *                  description, category, latitude, longitude, and optional metadata, encapsulated
     *                  in a CreatePoiRequest object. The request is validated on entry.
     * @return A ResponseEntity containing the created POI details in the body as a PoiAdminResponse object
     *         and the HTTP status code of 201 (Created). The response also includes the URI location of
     *         the created resource in the headers.
     */
    @PostMapping
    fun createPoi(@Valid @RequestBody createPoi: CreatePoiRequest): ResponseEntity<PoiAdminResponse> {
        val created = pointOfInterestService.createPoi(createPoi)
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id).toUri()

        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created)
    }

    /**
     * Updates an existing Point of Interest (POI) with the provided details.
     *
     * @param id The unique identifier of the Point of Interest to update.
     * @param updatePoi The new details for the Point of Interest, encapsulated in an `UpdatePoiRequest` object.
     * @return A `ResponseEntity` containing the updated Point of Interest as a `PoiAdminResponse`.
     */
    @PutMapping("/{id}")
    fun updatePoi(
        @PathVariable id: UUID,
        @Valid @RequestBody updatePoi: UpdatePoiRequest
    ): ResponseEntity<PoiAdminResponse> {
        val updated = pointOfInterestService.updatePoi(id, updatePoi)

        return ResponseEntity.ok(updated)
    }

    /**
     * Deletes a Point of Interest (POI) by its unique identifier.
     *
     * @param id The unique identifier of the Point of Interest to delete.
     * @return A `ResponseEntity` with no content if the deletion is successful.
     */
    @DeleteMapping("/{id}")
    fun deletePoi(@PathVariable id: UUID): ResponseEntity<Void> {
        pointOfInterestService.deletePoi(id)

        return ResponseEntity.noContent().build()
    }
}
