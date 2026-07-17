package com.example.restaurantops.table.model

import java.time.LocalDateTime
import java.util.UUID

data class RestaurantTable(
    val id: UUID,
    val storeId: UUID,
    val tableName: String,
    val seatCount: Int,
    val status: TableStatus,
    val createdAt: LocalDateTime,
)
