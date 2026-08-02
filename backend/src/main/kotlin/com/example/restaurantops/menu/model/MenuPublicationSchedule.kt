package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuPublicationSchedule(
    val id: UUID,
    val storeId: UUID,
    val menuItemId: UUID,
    val revisionId: UUID,
    val publishAt: LocalDateTime,
    val status: MenuPublicationScheduleStatus,
    val createdBy: UUID?,
    val createdAt: LocalDateTime,
    val processedAt: LocalDateTime?,
)