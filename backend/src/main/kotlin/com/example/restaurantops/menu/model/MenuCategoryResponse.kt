package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuCategoryResponse(
    val id: UUID,
    val storeId: UUID,
    val name: String,
    val displayOrder: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(menuCategory: MenuCategory): MenuCategoryResponse {
            return MenuCategoryResponse(
                id = menuCategory.id,
                storeId = menuCategory.storeId,
                name = menuCategory.name,
                displayOrder = menuCategory.displayOrder,
                createdAt = menuCategory.createdAt,
            )
        }
    }
}
