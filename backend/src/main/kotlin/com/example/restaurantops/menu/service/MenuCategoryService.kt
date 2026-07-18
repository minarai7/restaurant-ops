package com.example.restaurantops.menu.service

import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.menu.model.CreateMenuCategoryRequest
import com.example.restaurantops.menu.model.MenuCategoryResponse
import com.example.restaurantops.menu.repository.MenuCategoryRepository
import com.example.restaurantops.store.service.StoreService
import com.example.restaurantops.table.model.CreateRestaurantTableRequest
import com.example.restaurantops.table.model.RestaurantTableResponse
import com.example.restaurantops.table.model.TableStatus
import com.example.restaurantops.table.model.UpdateRestaurantTableStatusRequest
import com.example.restaurantops.table.repository.RestaurantTableRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MenuCategoryService (
    private val menuCategoryRepository: MenuCategoryRepository,
    private val storeService: StoreService,
) {
    fun createMenuCategory(
        storeId: UUID,
        request: CreateMenuCategoryRequest,
    ): MenuCategoryResponse {
        storeService.requireStore(storeId)
        
        val menuCategory = menuCategoryRepository.create(
            id = UUID.randomUUID(),
            storeId = storeId,
            name = request.name.trim(),
            displayOrder = request.displayOrder,
        )
        
        return MenuCategoryResponse.from(menuCategory)
    }
    
    fun getMenuCategories(
        storeId: UUID,
    ): List<MenuCategoryResponse> {
        storeService.requireStore(storeId)
        
        return menuCategoryRepository
            .findAllByStoreId(storeId)
            .map(MenuCategoryResponse::from)
    }
}