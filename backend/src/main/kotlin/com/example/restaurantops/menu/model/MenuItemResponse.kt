package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuItemResponse(
    val id: UUID,
    val storeId: UUID,
    val categoryId: UUID,
    val name: String,
    val price: Int,
    val isAvailable: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(menuItem: MenuItem): MenuItemResponse {
            return MenuItemResponse(
                id = menuItem.id,
                storeId = menuItem.storeId,
                categoryId = menuItem.categoryId,
                name = menuItem.name,
                price = menuItem.price,
                isAvailable = menuItem.isAvailable,
                createdAt = menuItem.createdAt,
                updatedAt = menuItem.updatedAt,
            )
        }
    }
}