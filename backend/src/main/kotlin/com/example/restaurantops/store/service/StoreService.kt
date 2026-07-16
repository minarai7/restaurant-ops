package com.example.restaurantops.store.service

import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.store.model.CreateStoreRequest
import com.example.restaurantops.store.model.StoreResponse
import com.example.restaurantops.store.repository.StoreRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StoreService (
    private val storeRepository: StoreRepository,
) {
    fun createStore(request: CreateStoreRequest): StoreResponse {
        val store = storeRepository.create(
            id = UUID.randomUUID(),
            name = request.name.trim(),
        )
        
        return StoreResponse.from(store)
    }
    
    fun getStores(): List<StoreResponse> {
        return storeRepository
            .findAll()
            .map(StoreResponse::from)
    }
    
    fun getStore(storeId: UUID): StoreResponse {
        val store = storeRepository.findById(storeId)
            ?: throw ResourceNotFoundException(
                message = "Store not found",
            )
        
        return StoreResponse.from(store)
    }
}