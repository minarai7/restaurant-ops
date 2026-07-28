package com.example.restaurantops.menu.model

import java.time.LocalDateTime
import java.util.UUID

data class MenuItemRevisionResponse(
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
) {
    companion object {
        fun from(revision: MenuItemRevision): MenuItemRevisionResponse {
            return MenuItemRevisionResponse(
                id = revision.id,
                menuItemId = revision.menuItemId,
                storeId = revision.storeId,
                revisionNumber = revision.revisionNumber,
                status = revision.status,
                name = revision.name,
                description = revision.description,
                price = revision.price,
                version = revision.version,
                createdBy = revision.createdBy,
                createdAt = revision.createdAt,
                publishedAt = revision.publishedAt,
            )
        }
    }
}
