package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuItem(
    val id: UUID,
    val storeId: UUID,
    val categoryId: UUID,
    val isAvailable: Boolean,
    val displayOrder: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)