package com.example.restaurantops.order.model

import java.util.UUID

data class CreateOrderRequest(
    val tableId: UUID,
)
