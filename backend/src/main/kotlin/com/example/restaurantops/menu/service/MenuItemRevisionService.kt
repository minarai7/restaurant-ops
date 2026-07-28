package com.example.restaurantops.menu.service

import com.example.restaurantops.common.error.ConflictException
import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.menu.model.CreateMenuItemDraftRequest
import com.example.restaurantops.menu.model.MenuItemRevisionResponse
import com.example.restaurantops.menu.repository.MenuItemRepository
import com.example.restaurantops.menu.repository.MenuItemRevisionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MenuItemRevisionService(
    private val menuItemRepository: MenuItemRepository,
    private val menuItemRevisionRepository: MenuItemRevisionRepository,
) {

    @Transactional
    fun createDraft(
        storeId: UUID,
        menuItemId: UUID,
        request: CreateMenuItemDraftRequest,
    ): MenuItemRevisionResponse {
        requireMenuItem(storeId, menuItemId)

        if (menuItemRevisionRepository.findDraftByMenuItemIdAndStoreId(menuItemId, storeId) != null) {
            throw ConflictException(
                message = "Menu item already has a draft",
            )
        }

        val revision = menuItemRevisionRepository.createDraft(
            id = UUID.randomUUID(),
            menuItemId = menuItemId,
            storeId = storeId,
            name = request.name.trim(),
            description = request.description?.trim(),
            price = request.price,
            createdBy = null,
        )

        return MenuItemRevisionResponse.from(revision)
    }

    fun getRevisions(
        storeId: UUID,
        menuItemId: UUID,
    ): List<MenuItemRevisionResponse> {
        requireMenuItem(storeId, menuItemId)

        return menuItemRevisionRepository
            .findAllByMenuItemIdAndStoreId(menuItemId, storeId)
            .map(MenuItemRevisionResponse::from)
    }

    fun getDraft(
        storeId: UUID,
        menuItemId: UUID,
    ): MenuItemRevisionResponse {
        requireMenuItem(storeId, menuItemId)

        val draft = menuItemRevisionRepository
            .findDraftByMenuItemIdAndStoreId(menuItemId, storeId)
            ?: throw ResourceNotFoundException(
                message = "Menu item has no draft",
            )

        return MenuItemRevisionResponse.from(draft)
    }

    private fun requireMenuItem(
        storeId: UUID,
        menuItemId: UUID,
    ) {
        menuItemRepository.findByIdAndStoreId(
            id = menuItemId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item not found",
        )
    }
}
