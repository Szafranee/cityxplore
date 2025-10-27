package org.cityxplore.backend.user.controller

import org.cityxplore.backend.shared.security.JwtUtils
import org.cityxplore.backend.user.dto.MeResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public endpoint exposing basic information about the currently authenticated user.
 *
 * This controller simply reflects selected claims from the access token and returns
 * them in a lightweight DTO. It uses [org.cityxplore.backend.shared.security.JwtUtils] for consistent user id extraction
 * across the application, matching conventions used in other controllers.
 */
@RestController
@RequestMapping("/api/me")
class MeController {

    /**
     * Returns the current user's basic identity data derived from the JWT.
     */
    @GetMapping
    fun getUser(@AuthenticationPrincipal jwt: Jwt): MeResponse {
        val userId = JwtUtils.extractUserId(jwt).toString()
        return MeResponse(
            userId = userId,
            email = jwt.claims["email"] as? String,
            role = jwt.claims["role"] as? String
        )
    }
}
