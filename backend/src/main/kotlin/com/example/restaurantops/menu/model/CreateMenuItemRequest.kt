package com.example.restaurantops.menu.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.util.UUID

data class CreateMenuItemRequest(
    val categoryId: UUID,

    @field:NotBlank(
        message = "Name must not be blank",
    )
    val name: String,

    @field:PositiveOrZero(
        message = "Price must be zero or greater",
    )
    val price: Int,

    val isAvailable: Boolean = true,
)