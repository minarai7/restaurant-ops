package com.example.restaurantops.order.model

import jakarta.validation.constraints.Positive

data class UpdateOrderItemRequest(
    @field:Positive(
        message = "Quantity must be greater than zero",
    )
    val quantity: Int,
)
