package org.cityxplore.backend.storage.controller

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.cityxplore.backend.storage.service.StorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for interacting with Supabase Storage.
 *
 * This controller provides endpoints for generating signed URLs and deleting files.
 * It leverages the [StorageService] to interact with the Supabase Storage API.
 */
@RestController
@RequestMapping("/api/storage")
class StorageController(
    private val storageService: StorageService
) {

    /**
     * Generates a signed URL for a given object path.
     */
    @GetMapping("/url")
    fun getSignedUrl(
        @RequestParam @NotBlank bucket: String,
        @RequestParam @NotBlank path: String,
        @RequestParam(required = false, defaultValue = "3600") @Min(60) expiresIn: Int
    ): ResponseEntity<Map<String, String>> {
        val signed = storageService.createSignedUrl(bucket, path, expiresIn)

        return ResponseEntity.ok(mapOf("signedUrl" to signed))
    }

    /**
     * Deletes a file in Supabase Storage.
     */
    @DeleteMapping
    fun deleteFile(
        @RequestParam @NotBlank bucket: String,
        @RequestParam @NotBlank path: String
    ): ResponseEntity<Void> {
        storageService.deleteFile(bucket, path)

        return ResponseEntity.noContent().build()
    }
}
