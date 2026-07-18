package com.example.restaurantops.menu.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDateTime
import java.util.UUID

data class CreateMenuCategoryRequest(
    @field:NotBlank(message = "Name must not be blank")
    val name: String,
    
    @field:PositiveOrZero(
        message = "Display order must be zero or greater",
    )
    val displayOrder: Int = 0,
)
