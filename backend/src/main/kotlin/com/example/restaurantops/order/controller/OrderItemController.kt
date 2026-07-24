package com.example.restaurantops.order.controller

import com.example.restaurantops.order.model.AddOrderItemRequest
import com.example.restaurantops.order.model.OrderItemResponse
import com.example.restaurantops.order.model.UpdateOrderItemRequest
import com.example.restaurantops.order.service.OrderItemService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/stores/{storeId}/orders/{orderId}/items")
class OrderItemController(
    private val orderItemService: OrderItemService,
) {
    @PostMapping
    fun addItem(
        @PathVariable storeId: UUID,
        @PathVariable orderId: UUID,
        @Valid @RequestBody request: AddOrderItemRequest,
    ): ResponseEntity<OrderItemResponse> {
        val response = orderItemService.addItem(
            storeId = storeId,
            orderId = orderId,
            request = request,
        )
        return ResponseEntity
            .created(
                URI.create(
                    "/api/stores/$storeId/orders/$orderId/items/${response.id}",
                )
            )
            .body(response)
    }

    @PatchMapping("/{orderItemId}")
    fun updateItem(
        @PathVariable storeId: UUID,
        @PathVariable orderId: UUID,
        @PathVariable orderItemId: UUID,
        @Valid @RequestBody request: UpdateOrderItemRequest,
    ): OrderItemResponse {
        return orderItemService.updateItem(
            storeId = storeId,
            orderId = orderId,
            orderItemId = orderItemId,
            request = request,
        )
    }

    @DeleteMapping("/{orderItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(
        @PathVariable storeId: UUID,
        @PathVariable orderId: UUID,
        @PathVariable orderItemId: UUID,
    ) {
        orderItemService.deleteItem(
            storeId = storeId,
            orderId = orderId,
            orderItemId = orderItemId,
        )
    }
}
