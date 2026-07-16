package com.example.restaurantops.store.controller

import com.example.restaurantops.store.model.CreateStoreRequest
import com.example.restaurantops.store.model.StoreResponse
import com.example.restaurantops.store.service.StoreService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/stores")
class StoreController (
    private val storeService: StoreService,
) {
    @PostMapping
    fun createStore(
        @Valid @RequestBody request: CreateStoreRequest,
    ): ResponseEntity<StoreResponse> {
        val response = storeService.createStore(request)
        
        return ResponseEntity
            .created(URI.create("/api/stores/${response.id}"))
            .body(response)
    }
    
    @GetMapping
    fun getStores(): List<StoreResponse> {
        return storeService.getStores()
    }
    
    @GetMapping("/{storeId}")
    fun getStore(
        @PathVariable storeId: UUID,
    ): StoreResponse {
        return storeService.getStore(storeId)
    }
}