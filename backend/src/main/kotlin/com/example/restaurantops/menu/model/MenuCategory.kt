package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuCategory(
    val id: UUID,
    val storeId: UUID,
    val name: String,
    val displayOrder: Int,
    val createdAt: LocalDateTime,
)
