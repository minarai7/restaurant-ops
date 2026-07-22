package com.example.restaurantops.order.model

import java.time.LocalDateTime
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val storeId: UUID,
    val tableId: UUID,
    val status: OrderStatus,
    val openedAt: LocalDateTime,
    val checkedOutAt: LocalDateTime?,
) {
    companion object {
        fun from(order: Order): OrderResponse {
            return OrderResponse(
                id = order.id,
                storeId = order.storeId,
                tableId = order.tableId,
                status = order.status,
                openedAt = order.openedAt,
                checkedOutAt = order.checkedOutAt,
            )
        }
    }
}
