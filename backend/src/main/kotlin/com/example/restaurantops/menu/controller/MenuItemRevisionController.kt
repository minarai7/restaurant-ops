package com.example.restaurantops.menu.controller

import com.example.restaurantops.menu.model.CreateMenuItemDraftRequest
import com.example.restaurantops.menu.model.MenuItemRevisionResponse
import com.example.restaurantops.menu.model.PublishMenuItemRequest
import com.example.restaurantops.menu.model.UpdateMenuItemDraftRequest
import com.example.restaurantops.menu.service.MenuItemRevisionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/stores/{storeId}/menu-items/{menuItemId}")
class MenuItemRevisionController(
    private val menuItemRevisionService: MenuItemRevisionService,
) {

    @PostMapping("/drafts")
    fun createDraft(
        @PathVariable storeId: UUID,
        @PathVariable menuItemId: UUID,
        @Valid @RequestBody request: CreateMenuItemDraftRequest,
    ): ResponseEntity<MenuItemRevisionResponse> {
        val response = menuItemRevisionService.createDraft(
            storeId = storeId,
            menuItemId = menuItemId,
            request = request,
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .location(
                URI.create(
                    "/api/stores/$storeId/menu-items/$menuItemId/draft",
                ),
            )
            .body(response)
    }

    @GetMapping("/revisions")
    fun getRevisions(
        @PathVariable storeId: UUID,
        @PathVariable menuItemId: UUID,
    ): List<MenuItemRevisionResponse> {
        return menuItemRevisionService.getRevisions(
            storeId = storeId,
            menuItemId = menuItemId,
        )
    }

    @GetMapping("/draft")
    fun getDraft(
        @PathVariable storeId: UUID,
        @PathVariable menuItemId: UUID,
    ): MenuItemRevisionResponse {
        return menuItemRevisionService.getDraft(
            storeId = storeId,
            menuItemId = menuItemId,
        )
    }

    @PatchMapping("/draft")
    fun updateDraft(
        @PathVariable storeId: UUID,
        @PathVariable menuItemId: UUID,
        @Valid @RequestBody request: UpdateMenuItemDraftRequest,
    ): MenuItemRevisionResponse {
        return menuItemRevisionService.updateDraft(
            storeId = storeId,
            menuItemId = menuItemId,
            request = request,
        )
    }

    @PostMapping("/publish")
    fun publish(
        @PathVariable storeId: UUID,
        @PathVariable menuItemId: UUID,
        @Valid @RequestBody request: PublishMenuItemRequest,
    ): MenuItemRevisionResponse {
        return menuItemRevisionService.publish(
            storeId = storeId,
            menuItemId = menuItemId,
            request = request,
        )
    }
}
