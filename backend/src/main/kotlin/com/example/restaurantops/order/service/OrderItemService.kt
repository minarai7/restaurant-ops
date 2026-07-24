package com.example.restaurantops.order.service

import com.example.restaurantops.common.error.ConflictException
import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.menu.repository.MenuItemRepository
import com.example.restaurantops.order.model.AddOrderItemRequest
import com.example.restaurantops.order.model.OrderItemResponse
import com.example.restaurantops.order.model.OrderStatus
import com.example.restaurantops.order.model.UpdateOrderItemRequest
import com.example.restaurantops.order.repository.OrderItemRepository
import com.example.restaurantops.order.repository.OrderRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderItemService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val menuItemRepository: MenuItemRepository,
) {
    fun addItem(
        storeId: UUID,
        orderId: UUID,
        request: AddOrderItemRequest,
    ): OrderItemResponse {
        val order = orderRepository.findByIdAndStoreId(
            id = orderId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException("Order not found")

        if (order.status != OrderStatus.OPEN) {
            throw ConflictException("Order is no longer open")
        }

        val menuItem = menuItemRepository.findByIdAndStoreId(
            id = request.menuItemId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException("Menu item not found")

        if (!menuItem.isAvailable) {
            throw ConflictException("Menu item is not available")
        }

        val orderItem = orderItemRepository.add(
            id = UUID.randomUUID(),
            orderId = orderId,
            menuItemId = menuItem.id,
            itemNameSnapshot = menuItem.name,
            unitPriceSnapshot = menuItem.price,
            quantity = request.quantity,
        )

        return OrderItemResponse.from(orderItem)
    }

    fun updateItem(
        storeId: UUID,
        orderId: UUID,
        orderItemId: UUID,
        request: UpdateOrderItemRequest,
    ): OrderItemResponse {
        val order = orderRepository.findByIdAndStoreId(
            id = orderId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException("Order not found")

        if (order.status != OrderStatus.OPEN) {
            throw ConflictException("Order is no longer open")
        }

        val orderItem = orderItemRepository.updateQuantity(
            id = orderItemId,
            orderId = orderId,
            quantity = request.quantity,
        ) ?: throw ResourceNotFoundException("Order item not found")

        return OrderItemResponse.from(orderItem)
    }

    fun deleteItem(
        storeId: UUID,
        orderId: UUID,
        orderItemId: UUID,
    ) {
        val order = orderRepository.findByIdAndStoreId(
            id = orderId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException("Order not found")

        if (order.status != OrderStatus.OPEN) {
            throw ConflictException("Order is no longer open")
        }

        val deleted = orderItemRepository.delete(
            id = orderItemId,
            orderId = orderId,
        )

        if (!deleted) {
            throw ResourceNotFoundException("Order item not found")
        }
    }
}
