package com.example.restaurantops.table.service

import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.store.repository.StoreRepository
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
    private val storeRepository: StoreRepository,
) {
    fun createTable(
        storeId: UUID,
        request: CreateRestaurantTableRequest,
    ): RestaurantTableResponse {
        requireStore(storeId)
        
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
        requireStore(storeId)
        
        return restaurantTableRepository
            .findAllByStoreId(storeId)
            .map(RestaurantTableResponse::from)
    }
    
    fun updateTableStatus(
        storeId: UUID,
        tableId: UUID,
        request: UpdateRestaurantTableStatusRequest,
    ): RestaurantTableResponse {
        requireStore(storeId)
        
        val table = restaurantTableRepository.updateStatus(
            storeId = storeId,
            tableId = tableId,
            status = request.status,
        ) ?: throw ResourceNotFoundException(
            "Table not found",
        )
        
        return RestaurantTableResponse.from(table)
    } 
    
    private fun requireStore(storeId: UUID) {
        storeRepository.findById(storeId)
            ?: throw ResourceNotFoundException(
                "Store not found"
            )
    }
}