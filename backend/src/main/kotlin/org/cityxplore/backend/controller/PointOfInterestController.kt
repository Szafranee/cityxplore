package org.cityxplore.backend.controller

import org.cityxplore.backend.entity.PointOfInterest
import org.cityxplore.backend.repository.PointOfInterestRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pois")
class PointOfInterestController(
    private val poiRepository: PointOfInterestRepository
) {

    @GetMapping
    fun getAllPOIs(): List<PointOfInterest> = poiRepository.findAll()

    @GetMapping("/{id}")
    fun getPOI(@PathVariable id: String): PointOfInterest =
        poiRepository.findById(java.util.UUID.fromString(id)).orElseThrow()

    @PostMapping
    fun createPOI(@RequestBody poi: PointOfInterest): PointOfInterest =
        poiRepository.save(poi)
}