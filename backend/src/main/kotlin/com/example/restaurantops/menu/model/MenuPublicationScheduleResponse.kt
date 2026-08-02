package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuPublicationScheduleResponse(
    val id: UUID,
    val storeId: UUID,
    val menuItemId: UUID,
    val revisionId: UUID,
    val publishAt: LocalDateTime,
    val status: MenuPublicationScheduleStatus,
    val createdBy: UUID?,
    val createdAt: LocalDateTime,
    val processedAt: LocalDateTime?,
) {
    companion object {
        fun from(schedule: MenuPublicationSchedule): MenuPublicationScheduleResponse {
            return MenuPublicationScheduleResponse(
                id = schedule.id,
                storeId = schedule.storeId,
                menuItemId = schedule.menuItemId,
                revisionId = schedule.revisionId,
                publishAt = schedule.publishAt,
                status = schedule.status,
                createdBy = schedule.createdBy,
                createdAt = schedule.createdAt,
                processedAt = schedule.processedAt,
            )
        }
    }
}