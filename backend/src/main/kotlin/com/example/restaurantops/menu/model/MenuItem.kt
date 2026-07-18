package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuItem(
    val id: UUID,
    val storeId: UUID,
    val categoryId: UUID,
    val name: String,
    val price: Int,
    val isAvailable: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)