package com.example.restaurantops.menu.controller

import com.example.restaurantops.menu.model.CreateMenuItemRequest
import com.example.restaurantops.menu.model.MenuItemResponse
import com.example.restaurantops.menu.model.UpdateMenuItemAvailabilityRequest
import com.example.restaurantops.menu.service.MenuItemService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/stores/{storeId}/menu-items")
class MenuItemController(
    private val menuItemService: MenuItemService,
) {

    @PostMapping
    fun createMenuItem(
        @PathVariable storeId: UUID,
        @Valid @RequestBody request: CreateMenuItemRequest,
    ): ResponseEntity<MenuItemResponse> {
        val response = menuItemService.createMenuItem(
            storeId = storeId,
            request = request,
        )
        return ResponseEntity
            .created(
                URI.create(
                    "/api/stores/$storeId/menu-items/${response.id}",
                )
            )
            .body(response)
    }

    @GetMapping
    fun getMenuItems(
        @PathVariable storeId: UUID,
    ): List<MenuItemResponse> {
        return menuItemService.getMenuItems(storeId)
    }

    @PatchMapping("/{menuItemId}/availability")
    fun updateAvailability(
        @PathVariable storeId: UUID,
        @PathVariable menuItemId: UUID,
        @Valid
        @RequestBody
        request: UpdateMenuItemAvailabilityRequest,
    ): MenuItemResponse {
        return menuItemService.updateAvailability(
            storeId = storeId,
            menuItemId = menuItemId,
            request = request,
        )
    }
}