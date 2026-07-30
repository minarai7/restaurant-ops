package com.example.restaurantops.menu.controller

import com.example.restaurantops.menu.model.MenuItemResponse
import com.example.restaurantops.menu.model.ReorderMenuItemsRequest
import com.example.restaurantops.menu.service.MenuItemOrderingService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/stores/{storeId}/menu-categories/{categoryId}/menu-item-order")
class MenuItemOrderController(
    private val menuItemOrderingService: MenuItemOrderingService,
) {

    @PutMapping
    fun reorder(
        @PathVariable storeId: UUID,
        @PathVariable categoryId: UUID,
        @Valid @RequestBody request: ReorderMenuItemsRequest,
    ): List<MenuItemResponse> {
        return menuItemOrderingService.reorder(
            storeId = storeId,
            categoryId = categoryId,
            request = request,
        )
    }
}
