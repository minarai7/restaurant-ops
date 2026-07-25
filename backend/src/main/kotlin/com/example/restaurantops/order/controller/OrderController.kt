package com.example.restaurantops.order.controller

import com.example.restaurantops.order.model.CheckoutRequest
import com.example.restaurantops.order.model.CheckoutResponse
import com.example.restaurantops.order.model.CreateOrderRequest
import com.example.restaurantops.order.model.OrderResponse
import com.example.restaurantops.order.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/stores/{storeId}/orders")
class OrderController (
    private val orderService: OrderService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @PathVariable storeId: UUID,
        @RequestBody request: CreateOrderRequest,
    ): ResponseEntity<OrderResponse> {
        val response = orderService.createOrder(
            storeId = storeId,
            request = request,
        )
        
        return ResponseEntity
            .created(
                URI.create(
                    "/api/stores/$storeId/orders/${response.id}",
                )
            )
            .body(response)
    }
    
    @GetMapping("/{orderId}")
    fun getOrder(
        @PathVariable storeId: UUID,
        @PathVariable orderId: UUID,
    ): OrderResponse {
        return orderService.getOrder(
            storeId = storeId,
            orderId = orderId,
        )
    }

    @PostMapping("/{orderId}/checkout")
    @ResponseStatus(HttpStatus.OK)
    fun checkout(
        @PathVariable storeId: UUID,
        @PathVariable orderId: UUID,
        @RequestBody request: CheckoutRequest,
    ): CheckoutResponse {
        return orderService.checkout(
            storeId = storeId,
            orderId = orderId,
            request = request,
        )
    }
}