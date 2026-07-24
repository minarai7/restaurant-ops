package com.example.restaurantops.order.model

import java.time.LocalDateTime
import java.util.UUID

data class OrderItem(
    val id: UUID,
    val orderId: UUID,
    val menuItemId: UUID?,
    val itemNameSnapshot: String,
    val unitPriceSnapshot: Int,
    val quantity: Int,
    val createdAt: LocalDateTime,
)
