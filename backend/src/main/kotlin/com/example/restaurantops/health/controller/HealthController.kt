package com.example.restaurantops.health.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    
    @GetMapping("/health")
    fun health(): HealthResponse {
        return HealthResponse(status = "ok")
    }
}

data class HealthResponse(
    val status: String
)