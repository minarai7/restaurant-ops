package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuItemRevision(
    val id: UUID,
    val menuItemId: UUID,
    val storeId: UUID,
    val revisionNumber: Int,
    val status: MenuItemRevisionStatus,
    val name: String,
    val description: String?,
    val price: Int,
    val version: Int,
    val createdBy: UUID?,
    val createdAt: LocalDateTime,
    val publishedAt: LocalDateTime?,
)