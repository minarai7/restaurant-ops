package com.example.restaurantops.health

@RestController
class HealthController {
    @GetMapping("/health")
    fun health(): HealthResponse {
        return HealthResponse(status = "ok")
    }
}

data class HealthResponse {
    val status: string
}