package com.example.restaurantops.menu.service

import com.example.restaurantops.common.error.ConflictException
import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.common.error.StaleVersionException
import com.example.restaurantops.menu.model.CreateMenuItemDraftRequest
import com.example.restaurantops.menu.model.MenuItemRevisionResponse
import com.example.restaurantops.menu.model.PublishMenuItemRequest
import com.example.restaurantops.menu.model.UpdateMenuItemDraftRequest
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

    @Transactional
    fun updateDraft(
        storeId: UUID,
        menuItemId: UUID,
        request: UpdateMenuItemDraftRequest,
    ): MenuItemRevisionResponse {
        requireMenuItem(storeId, menuItemId)

        val updated = menuItemRevisionRepository.updateDraft(
            storeId = storeId,
            menuItemId = menuItemId,
            name = request.name.trim(),
            description = request.description?.trim(),
            price = request.price,
            expectedVersion = requireNotNull(request.expectedVersion),
        )

        if (updated != null) {
            return MenuItemRevisionResponse.from(updated)
        }
        
        val draft = menuItemRevisionRepository.findDraftByMenuItemIdAndStoreId(
            menuItemId,
            storeId,
        ) ?: throw ResourceNotFoundException("Draft not found")
        
        throw StaleVersionException("Draft has been updated by another user")
    }

    @Transactional
    fun publish(
        storeId: UUID,
        menuItemId: UUID,
        request: PublishMenuItemRequest,
    ): MenuItemRevisionResponse {
        // Locked in this fixed order (menu_items, then the draft revision) so that
        // two concurrent publish calls can never deadlock against each other.
        menuItemRepository.lockByIdAndStoreId(
            id = menuItemId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item not found",
        )

        val draft = menuItemRevisionRepository.lockDraftByMenuItemIdAndStoreId(
            menuItemId = menuItemId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item has no draft",
        )
        
        if (draft.version != request.expectedVersion) {
            throw StaleVersionException(
                message = "Draft has been updated by another user"
            )
        }
        
        menuItemRevisionRepository.archivePublished(
            menuItemId = menuItemId,
            storeId = storeId,
        )
        
        val published = menuItemRevisionRepository.publishDraft(
            id = draft.id
        )
        
        menuItemRevisionRepository.createDraft(
            id = UUID.randomUUID(),
            menuItemId = menuItemId,
            storeId = storeId,
            name = published.name,
            description = published.description,
            price = published.price,
            createdBy = published.createdBy,
            version = published.version + 1,
        )
        
        return MenuItemRevisionResponse.from(published)
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
