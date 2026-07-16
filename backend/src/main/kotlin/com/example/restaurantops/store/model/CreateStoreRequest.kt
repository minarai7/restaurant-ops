package com.example.restaurantops.store.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateStoreRequest(
    @field:NotBlank(message = "Name must not be blank")
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String,
)
