package com.example.restaurantops.order.model

import jakarta.validation.constraints.Positive
import java.util.UUID

data class AddOrderItemRequest(
    val menuItemId: UUID,

    @field:Positive(
        message = "Quantity must be greater than zero",
    )
    val quantity: Int,
)
