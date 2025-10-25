package org.cityxplore.backend.controller

import org.cityxplore.backend.service.StorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Minimal controller to interact with Supabase Storage.
 * (Used for signed URLs and file deletions)
 */
@RestController
@RequestMapping("/api/storage")
class StorageController(
    private val storageService: StorageService
) {

    @GetMapping("/url")
    fun getSignedUrl(
        @RequestParam bucket: String,
        @RequestParam path: String,
        @RequestParam(required = false, defaultValue = "3600") expiresIn: Int
    ): ResponseEntity<Map<String, String>> {
        val signed = storageService.createSignedUrl(bucket, path, expiresIn)

        return ResponseEntity.ok(mapOf("signedUrl" to signed))
    }

    @DeleteMapping
    fun deleteFile(
        @RequestParam bucket: String,
        @RequestParam path: String
    ): ResponseEntity<Void> {
        storageService.deleteFile(bucket, path)

        return ResponseEntity.noContent().build()
    }
}
