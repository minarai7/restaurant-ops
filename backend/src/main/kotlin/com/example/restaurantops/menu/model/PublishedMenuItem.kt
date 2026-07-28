package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class PublishedMenuItem(
    val id: UUID,
    val storeId: UUID,
    val categoryId: UUID,
    val revisionId: UUID,
    val revisionNumber: Int,
    val name: String,
    val description: String?,
    val price: Int,
    val isAvailable: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val publishedAt: LocalDateTime,
)