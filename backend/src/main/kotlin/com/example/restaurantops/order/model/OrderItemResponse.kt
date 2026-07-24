package com.example.restaurantops.order.model

import java.time.LocalDateTime
import java.util.UUID

data class OrderItemResponse(
    val id: UUID,
    val orderId: UUID,
    val menuItemId: UUID?,
    val itemNameSnapshot: String,
    val unitPriceSnapshot: Int,
    val quantity: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(orderItem: OrderItem): OrderItemResponse {
            return OrderItemResponse(
                id = orderItem.id,
                orderId = orderItem.orderId,
                menuItemId = orderItem.menuItemId,
                itemNameSnapshot = orderItem.itemNameSnapshot,
                unitPriceSnapshot = orderItem.unitPriceSnapshot,
                quantity = orderItem.quantity,
                createdAt = orderItem.createdAt,
            )
        }
    }
}
