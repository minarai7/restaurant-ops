package com.example.restaurantops.store.model

import java.time.LocalDateTime
import java.util.UUID

data class Store(
    val id: UUID,
    val name: String,
    val createdAt: LocalDateTime,
)
