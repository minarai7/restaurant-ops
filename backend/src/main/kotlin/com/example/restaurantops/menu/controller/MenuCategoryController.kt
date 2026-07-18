package com.example.restaurantops.menu.controller

import com.example.restaurantops.menu.model.CreateMenuCategoryRequest
import com.example.restaurantops.menu.model.MenuCategoryResponse
import com.example.restaurantops.menu.service.MenuCategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/stores/{storeId}/menu-categories")
class MenuCategoryController(
    private val menuCategoryService: MenuCategoryService,
) {

    @PostMapping
    fun createMenuCategory(
        @PathVariable storeId: UUID,
        @Valid @RequestBody request: CreateMenuCategoryRequest,
    ): ResponseEntity<MenuCategoryResponse> {
        val response = menuCategoryService.createMenuCategory(
            storeId = storeId,
            request = request,
        )
        
        return ResponseEntity
            .created(
                URI.create(
                    "/api/stores/$storeId/menu-categories/${response.id}",
                )
            )
            .body(response)
    }

    @GetMapping
    fun getMenuCategories(
        @PathVariable storeId: UUID,
    ): List<MenuCategoryResponse> {
        return menuCategoryService.getMenuCategories(storeId)
    }
}