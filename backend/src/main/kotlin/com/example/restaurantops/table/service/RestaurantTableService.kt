package com.example.restaurantops.table.service

import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.store.service.StoreService
import com.example.restaurantops.table.model.CreateRestaurantTableRequest
import com.example.restaurantops.table.model.RestaurantTableResponse
import com.example.restaurantops.table.model.TableStatus
import com.example.restaurantops.table.model.UpdateRestaurantTableStatusRequest
import com.example.restaurantops.table.repository.RestaurantTableRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RestaurantTableService (
    private val restaurantTableRepository: RestaurantTableRepository,
    private val storeService: StoreService,
) {
    fun createTable(
        storeId: UUID,
        request: CreateRestaurantTableRequest,
    ): RestaurantTableResponse {
        storeService.requireStore(storeId)
        
        val table = restaurantTableRepository.create(
            id = UUID.randomUUID(),
                storeId = storeId,
                tableName = request.tableName.trim(),
                seatCount = request.seatCount,
                    status = TableStatus.EMPTY,
        )
        
        return RestaurantTableResponse.from(table)
    }
    
    fun getTables(
        storeId: UUID,
    ): List<RestaurantTableResponse> {
        storeService.requireStore(storeId)
        
        return restaurantTableRepository
            .findAllByStoreId(storeId)
            .map(RestaurantTableResponse::from)
    }
    
    fun updateTableStatus(
        storeId: UUID,
        tableId: UUID,
        request: UpdateRestaurantTableStatusRequest,
    ): RestaurantTableResponse {
        storeService.requireStore(storeId)
        
        val table = restaurantTableRepository.updateStatus(
            storeId = storeId,
            tableId = tableId,
            status = request.status,
        ) ?: throw ResourceNotFoundException(
            "Table not found",
        )
        
        return RestaurantTableResponse.from(table)
    } 
}