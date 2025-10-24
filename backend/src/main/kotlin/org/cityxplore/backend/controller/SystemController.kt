package org.cityxplore.backend.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public system endpoints (health-like and environment info).
 *
 * Kept minimal and public; aligns with security config which permits endpoints under /api/public.
 */
@RestController
@RequestMapping("/api/public")
class SystemController(

    @Value("\${spring.application.name:cityxplore-backend}")
    private val appName: String,

    @Value("\${spring.profiles.active:undefined}")
    private val activeProfile: String,

    @Value("\${app.version:local}")
    private val version: String
) {

    /**
     * Provides a simple health check endpoint to verify the application's availability and status.
     *
     * @return A ResponseEntity containing a map with the following keys:
     * - `status`: "OK" indicating the application is running
     * - `service`: the name of the application
     * - `timestamp`: the current server timestamp in milliseconds
     */
    @GetMapping("/ping")
    fun ping(): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(
            mapOf(
                "status" to "ok",
                "service" to appName,
                "timestamp" to System.currentTimeMillis()
            )
        )

    /**
     * Retrieves the application's active environment details.
     *
     * @return A ResponseEntity containing a map with the following keys:
     * - `profile`: the active Spring profile (e.g., "dev", "prod")
     * - `version`: the current version of the application
     */
    @GetMapping("/environment")
    fun environment(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(
            mapOf(
                "profile" to activeProfile,
                "version" to version
            )
        )
}
