package com.example.restaurantops.menu.service

import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.menu.model.CreateMenuItemRequest
import com.example.restaurantops.menu.model.MenuItemResponse
import com.example.restaurantops.menu.model.UpdateMenuItemAvailabilityRequest
import com.example.restaurantops.menu.repository.MenuCategoryRepository
import com.example.restaurantops.menu.repository.MenuItemRepository
import com.example.restaurantops.menu.repository.MenuItemRevisionRepository
import com.example.restaurantops.store.service.StoreService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MenuItemService(
    private val menuItemRepository: MenuItemRepository,
    private val menuItemRevisionRepository: MenuItemRevisionRepository,
    private val menuCategoryRepository: MenuCategoryRepository,
    private val storeService: StoreService,
) {

    @Transactional
    fun createMenuItem(
        storeId: UUID,
        request: CreateMenuItemRequest,
    ): MenuItemResponse {
        storeService.requireStore(storeId)

        requireCategory(
            storeId = storeId,
            categoryId = request.categoryId,
        )

        val menuItem = menuItemRepository.create(
            id = UUID.randomUUID(),
            storeId = storeId,
            categoryId = request.categoryId,
            isAvailable = request.isAvailable,
        )

        menuItemRevisionRepository.createDraft(
            id = UUID.randomUUID(),
            menuItemId = menuItem.id,
            storeId = storeId,
            name = request.name.trim(),
            description = request.description?.trim(),
            price = request.price,
            createdBy = null,
        )

        return MenuItemResponse.from(menuItem)
    }

    fun getMenuItems(
        storeId: UUID,
    ): List<MenuItemResponse> {
        storeService.requireStore(storeId)

        return menuItemRepository
            .findAllByStoreId(storeId)
            .map(MenuItemResponse::from)
    }

    fun updateAvailability(
        storeId: UUID,
        menuItemId: UUID,
        request: UpdateMenuItemAvailabilityRequest,
    ): MenuItemResponse {
        val isAvailable = requireNotNull(
            request.isAvailable,
        )

        val menuItem = menuItemRepository.updateAvailability(
            storeId = storeId,
            menuItemId = menuItemId,
            isAvailable = isAvailable,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item not found",
        )

        return MenuItemResponse.from(menuItem)
    }

    private fun requireCategory(
        storeId: UUID,
        categoryId: UUID,
    ) {
        menuCategoryRepository.findByIdAndStoreId(
            categoryId = categoryId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu category not found",
        )
    }
}