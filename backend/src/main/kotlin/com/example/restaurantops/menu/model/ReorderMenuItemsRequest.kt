package com.example.restaurantops.menu.model

import jakarta.validation.constraints.NotEmpty
import java.util.UUID

data class ReorderMenuItemsRequest(
    @field:NotEmpty(
        message = "Menu item ids must not be empty",
    )
    val menuItemIds: List<UUID>,
)
