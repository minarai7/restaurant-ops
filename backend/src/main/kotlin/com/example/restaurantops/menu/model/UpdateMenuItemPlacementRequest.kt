package com.example.restaurantops.menu.model

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.util.UUID

data class UpdateMenuItemPlacementRequest(
    @field:NotNull(
        message = "Category id is required",
    )
    val categoryId: UUID?,

    @field:NotNull(
        message = "Position is required",
    )
    @field:PositiveOrZero(
        message = "Position must be zero or greater",
    )
    val position: Int?,
)
