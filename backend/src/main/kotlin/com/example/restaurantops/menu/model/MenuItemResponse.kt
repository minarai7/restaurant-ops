package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuItemResponse(
    val id: UUID,
    val storeId: UUID,
    val categoryId: UUID,
    val isAvailable: Boolean,
    val displayOrder: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(menuItem: MenuItem): MenuItemResponse {
            return MenuItemResponse(
                id = menuItem.id,
                storeId = menuItem.storeId,
                categoryId = menuItem.categoryId,
                isAvailable = menuItem.isAvailable,
                displayOrder = menuItem.displayOrder,
                createdAt = menuItem.createdAt,
                updatedAt = menuItem.updatedAt,
            )
        }
    }
}