package com.example.restaurantops.menu.service

import com.example.restaurantops.common.error.ConflictException
import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.menu.model.MenuItemResponse
import com.example.restaurantops.menu.model.ReorderMenuItemsRequest
import com.example.restaurantops.menu.model.UpdateMenuItemPlacementRequest
import com.example.restaurantops.menu.repository.MenuCategoryRepository
import com.example.restaurantops.menu.repository.MenuItemRepository
import com.example.restaurantops.store.service.StoreService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MenuItemOrderingService(
    private val menuItemRepository: MenuItemRepository,
    private val menuCategoryRepository: MenuCategoryRepository,
    private val storeService: StoreService,
) {

    @Transactional
    fun reorder(
        storeId: UUID,
        categoryId: UUID,
        request: ReorderMenuItemsRequest,
    ): List<MenuItemResponse> {
        storeService.requireStore(storeId)

        // Locking the category row (not the item rows) is what serializes two
        // concurrent reorders of the same category: the set of items being
        // reordered can differ between requests, so there's no stable item-row
        // lock target to use instead.
        menuCategoryRepository.lockByIdAndStoreId(
            categoryId = categoryId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu category not found",
        )

        menuItemRepository.deferPositionConstraint()

        val currentIds = menuItemRepository.findIdsByCategoryIdAndStoreId(
            categoryId = categoryId,
            storeId = storeId,
        )

        validateReorderRequest(
            currentIds = currentIds,
            submittedIds = request.menuItemIds,
            storeId = storeId,
        )

        menuItemRepository.applyOrder(
            categoryId = categoryId,
            storeId = storeId,
            menuItemIds = request.menuItemIds,
        )

        return menuItemRepository
            .findAllByCategoryIdAndStoreId(categoryId, storeId)
            .map(MenuItemResponse::from)
    }

    @Transactional
    fun updatePlacement(
        storeId: UUID,
        menuItemId: UUID,
        request: UpdateMenuItemPlacementRequest,
    ): MenuItemResponse {
        storeService.requireStore(storeId)

        val destinationCategoryId = requireNotNull(request.categoryId)
        val position = requireNotNull(request.position)

        val menuItem = menuItemRepository.findByIdAndStoreId(
            id = menuItemId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item not found",
        )

        menuCategoryRepository.findByIdAndStoreId(
            categoryId = destinationCategoryId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu category not found",
        )

        val sourceCategoryId = menuItem.categoryId

        // Always lock in ascending UUID order, never request order, so a
        // concurrent A -> B move and a concurrent B -> A move queue behind
        // each other on the same first lock instead of each holding one
        // category and waiting on the other.
        setOf(sourceCategoryId, destinationCategoryId)
            .sortedBy { it }
            .forEach { lockCategoryId ->
                menuCategoryRepository.lockByIdAndStoreId(
                    categoryId = lockCategoryId,
                    storeId = storeId,
                )
            }

        // Re-read now that both locks are held: another request may have
        // already moved this item since the read above.
        val lockedMenuItem = menuItemRepository.findByIdAndStoreId(
            id = menuItemId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item not found",
        )

        if (lockedMenuItem.categoryId != sourceCategoryId) {
            throw ConflictException(
                message = "Menu item was moved by another request, please retry",
            )
        }

        menuItemRepository.deferPositionConstraint()

        if (sourceCategoryId == destinationCategoryId) {
            val reordered = menuItemRepository
                .findIdsByCategoryIdAndStoreId(destinationCategoryId, storeId)
                .toMutableList()
                .apply {
                    remove(menuItemId)
                    add(position.coerceAtMost(size), menuItemId)
                }

            menuItemRepository.applyOrder(
                categoryId = destinationCategoryId,
                storeId = storeId,
                menuItemIds = reordered,
            )
        } else {
            // Park the item in its new category at a placeholder position;
            // the applyOrder call just below immediately renumbers it (and
            // everything else in the category) to a dense 0-based order, so
            // the placeholder value itself is never observable.
            menuItemRepository.moveToCategory(
                menuItemId = menuItemId,
                storeId = storeId,
                categoryId = destinationCategoryId,
                displayOrder = Int.MAX_VALUE,
            )

            val destinationIds = menuItemRepository
                .findIdsByCategoryIdAndStoreId(destinationCategoryId, storeId)
                .toMutableList()
                .apply {
                    remove(menuItemId)
                    add(position.coerceAtMost(size), menuItemId)
                }

            menuItemRepository.applyOrder(
                categoryId = destinationCategoryId,
                storeId = storeId,
                menuItemIds = destinationIds,
            )

            val sourceIds = menuItemRepository.findIdsByCategoryIdAndStoreId(
                sourceCategoryId,
                storeId,
            )

            menuItemRepository.applyOrder(
                categoryId = sourceCategoryId,
                storeId = storeId,
                menuItemIds = sourceIds,
            )
        }

        return menuItemRepository.findByIdAndStoreId(menuItemId, storeId)
            ?.let(MenuItemResponse::from)
            ?: throw ResourceNotFoundException(
                message = "Menu item not found",
            )
    }

    // TODO(human): Implement the reorder validation policy.
    //
    // `currentIds` is every item currently in the category (in its existing
    // display order). `submittedIds` is the client's requested order. The
    // roadmap requires the request to be rejected up front, before applyOrder
    // runs, in these cases:
    //   - an id in `submittedIds` does not belong to this category/store
    //   - `submittedIds` is missing an id that belongs to this category
    //   - `submittedIds` contains a duplicate
    //
    // You get to decide which failures map to which exception:
    //   - ResourceNotFoundException(message) -> 404
    //   - ConflictException(message)         -> 409
    // (both already imported above). Consider: is "unknown item" the same
    // failure as "item from another store", from the client's point of view?
    // Is a short list a 404 (something's missing) or a 409 (request doesn't
    // match server state)? There's no single right answer — pick one and be
    // able to justify it.
    private fun validateReorderRequest(
        currentIds: List<UUID>,
        submittedIds: List<UUID>,
        storeId: UUID,
    ) {
        val currentIdsSet = currentIds.toSet()
        val submittedIdsSet = submittedIds.toSet()

        if (currentIds.size == submittedIds.size &&
            currentIdsSet == submittedIdsSet
        ) {
            return
        }

        val diff = (currentIdsSet - submittedIdsSet) union (submittedIdsSet - currentIdsSet)
        diff.forEach { menuItemId ->
            menuItemRepository.findByIdAndStoreId(
                id = menuItemId,
                storeId = storeId,
            ) ?: throw ResourceNotFoundException(
                message = "Menu item not found",
            )
        }

        throw ConflictException(
            message = "Menu item list is stale, please refetch and retry",
        )
    }
}
