package org.cityxplore.backend.controller

import org.cityxplore.backend.dto.response.MeResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/me")
class MeController {

    @GetMapping
    fun getUser(@AuthenticationPrincipal jwt: Jwt): MeResponse {
        return MeResponse(
            userId = jwt.claims["sub"] as? String,
            email = jwt.claims["email"] as? String,
            role = jwt.claims["role"] as? String
        )
    }
}
