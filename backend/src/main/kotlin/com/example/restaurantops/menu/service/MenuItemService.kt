package com.example.restaurantops.menu.service

import com.example.restaurantops.common.error.ConflictException
import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.menu.model.CreateMenuItemRequest
import com.example.restaurantops.menu.model.MenuItemResponse
import com.example.restaurantops.menu.model.UpdateMenuItemAvailabilityRequest
import com.example.restaurantops.menu.repository.MenuCategoryRepository
import com.example.restaurantops.menu.repository.MenuItemRepository
import com.example.restaurantops.store.service.StoreService
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MenuItemService(
    private val menuItemRepository: MenuItemRepository,
    private val menuCategoryRepository: MenuCategoryRepository,
    private val storeService: StoreService,
) {

    fun createMenuItem(
        storeId: UUID,
        request: CreateMenuItemRequest,
    ): MenuItemResponse {
        storeService.requireStore(storeId)

        requireCategory(
            storeId = storeId,
            categoryId = request.categoryId,
        )

        val menuItem = try {
            menuItemRepository.create(
                id = UUID.randomUUID(),
                storeId = storeId,
                categoryId = request.categoryId,
                name = request.name.trim(),
                price = request.price,
                isAvailable = request.isAvailable,
            )
        } catch (exception: DuplicateKeyException) {
            throw ConflictException(
                message = "Menu item name already exists",
            )
        }

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