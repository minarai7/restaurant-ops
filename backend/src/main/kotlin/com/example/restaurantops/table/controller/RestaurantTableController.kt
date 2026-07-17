package com.example.restaurantops.table.controller

import com.example.restaurantops.table.model.CreateRestaurantTableRequest
import com.example.restaurantops.table.model.RestaurantTableResponse
import com.example.restaurantops.table.model.UpdateRestaurantTableStatusRequest
import com.example.restaurantops.table.service.RestaurantTableService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/stores/{storeId}/tables")
class RestaurantTableController (
    private val restaurantTableService: RestaurantTableService,
) {
    @PostMapping
    fun createTable(
        @PathVariable storeId: UUID,
        @Valid @RequestBody request: CreateRestaurantTableRequest,
    ): ResponseEntity<RestaurantTableResponse> {
        val response = restaurantTableService.createTable(
            storeId = storeId,
            request = request,
        )
        
        return ResponseEntity
            .created(
                URI.create(
                    "/api/store/$storeId/tables/${response.id}",
                )
            )
            .body(response)
    }
    
    @GetMapping
    fun getTables(
        @PathVariable storeId: UUID,
    ): List<RestaurantTableResponse> {
        return restaurantTableService.getTables(storeId)
    }
    
    @PatchMapping("/{tableId}/status")
    fun updateTableStatus(
        @PathVariable storeId: UUID,
        @PathVariable tableId: UUID,
        @RequestBody request: UpdateRestaurantTableStatusRequest,
    ): RestaurantTableResponse {
        return restaurantTableService.updateTableStatus(
            storeId = storeId,
            tableId = tableId,
            request = request,
        )
    }
}