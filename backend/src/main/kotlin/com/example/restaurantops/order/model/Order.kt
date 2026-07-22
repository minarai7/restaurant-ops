package com.example.restaurantops.order.model

import java.time.LocalDateTime
import java.util.UUID

data class Order(
    val id: UUID,
    val storeId: UUID,
    val tableId: UUID,
    val status: OrderStatus,
    val openedAt: LocalDateTime,
    val checkedOutAt: LocalDateTime?,
)
