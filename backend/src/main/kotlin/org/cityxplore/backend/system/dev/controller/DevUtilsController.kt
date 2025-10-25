package org.cityxplore.backend.system.dev.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Utility / debug endpoints (available only on 'dev' profile).
 *
 * Exposes lightweight diagnostics without ujawniania sekretów/konfiguracji wprost.
 */
@RestController
@RequestMapping("/api/public/info")
@Profile("dev")
class DevUtilsController(
    @Value("\${spring.application.name}") private val appName: String,
    @Value("\${spring.profiles.active:dev}") private val profile: String,
    @Value("\${app.version:local}")
    private val version: String
) {

    /**
     * Returns basic application identity and active profile.
     */
    @GetMapping("/version")
    fun version() = mapOf(
        "app" to appName,
        "version" to version,
        "profile" to profile
    )

    /**
     * Minimal configuration sanity check; does not expose secret values.
     */
    @GetMapping("/config-check")
    fun configCheck(): Map<String, Any> =
        mapOf(
            "JAVA_HOME.present" to (System.getenv("JAVA_HOME") != null),
            "SPRING_PROFILES_ACTIVE" to (System.getenv("SPRING_PROFILES_ACTIVE") ?: profile),
            "DB_URL.present" to (System.getenv("DB_URL") != null),
            "timestamp" to System.currentTimeMillis()
        )
}
