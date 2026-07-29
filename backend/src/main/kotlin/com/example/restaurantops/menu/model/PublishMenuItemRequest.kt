package com.example.restaurantops.menu.model

import jakarta.validation.constraints.NotNull

data class PublishMenuItemRequest(
    @field:NotNull(
        message = "Expected version is required",
    )
    val expectedVersion: Int?,
)
