package com.example.restaurantops.menu.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero

data class CreateMenuItemDraftRequest(
    @field:NotBlank(
        message = "Name must not be blank",
    )
    val name: String,

    val description: String? = null,

    @field:PositiveOrZero(
        message = "Price must be zero or greater",
    )
    val price: Int,
)
