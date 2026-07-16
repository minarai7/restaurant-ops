package com.example.restaurantops.store.model

import java.time.LocalDateTime
import java.util.UUID

data class StoreResponse(
    val id: UUID,
    val name: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(store: Store): StoreResponse {
            return StoreResponse(
                id = store.id,
                name = store.name,
                createdAt = store.createdAt,
            )
        }
    }
}