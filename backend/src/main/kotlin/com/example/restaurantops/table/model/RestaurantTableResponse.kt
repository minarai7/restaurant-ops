package com.example.restaurantops.table.model

import java.time.LocalDateTime
import java.util.UUID

data class RestaurantTableResponse(
    val id: UUID,
    val storeId: UUID,
    val tableName: String,
    val seatCount: Int,
    val status: TableStatus,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(table: RestaurantTable): RestaurantTableResponse {
            return RestaurantTableResponse(
                id = table.id,
                storeId = table.storeId,
                tableName = table.tableName,
                seatCount = table.seatCount,
                status = table.status,
                createdAt = table.createdAt,
            )
        }
    }
}
