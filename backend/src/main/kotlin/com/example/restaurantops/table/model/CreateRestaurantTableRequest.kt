package com.example.restaurantops.table.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateRestaurantTableRequest(
    @field:NotBlank(message = "Table name must not be blank")
    @field:Size(
        max = 50,
        message = "Table name must be at most 50 characters",
    )
    val tableName: String,
    
    @field:Positive(
        message = "Seat count must be greater than 0",
    )
    val seatCount: Int,
)
