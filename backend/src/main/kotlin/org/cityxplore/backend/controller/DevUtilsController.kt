package org.cityxplore.backend.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Utility / debug endpoints.
 */
@RestController
@RequestMapping("/api/public/info")
class DevUtilsController(
    @Value("\${spring.application.name}") private val appName: String,
    @Value("\${spring.profiles.active:dev}") private val profile: String,
    @Value("\${app.version:local}")
    private val version: String
) {

    @GetMapping("/version")
    fun version() = mapOf(
        "app" to appName,
        "version" to version,
        "profile" to profile
    )

    @GetMapping("/config-check")
    fun configCheck(): Map<String, Any> =
        mapOf(
            "JAVA_HOME" to System.getenv("JAVA_HOME"),
            "SPRING_PROFILES_ACTIVE" to System.getenv("SPRING_PROFILES_ACTIVE"),
            "DB_URL" to (System.getenv("DB_URL") ?: "ok (hidden)"),
            "timestamp" to System.currentTimeMillis()
        )
}
