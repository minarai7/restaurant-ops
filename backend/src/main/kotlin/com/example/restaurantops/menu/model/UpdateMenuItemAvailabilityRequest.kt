package com.example.restaurantops.menu.model

import jakarta.validation.constraints.NotNull

data class UpdateMenuItemAvailabilityRequest(
    @field:NotNull(
        message = "Availability is required",
    )
    val isAvailable: Boolean?,
)