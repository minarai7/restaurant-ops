package com.example.restaurantops.order.service

import com.example.restaurantops.common.error.ConflictException
import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.order.model.CreateOrderRequest
import com.example.restaurantops.order.model.OrderResponse
import com.example.restaurantops.order.repository.OrderRepository
import com.example.restaurantops.store.service.StoreService
import com.example.restaurantops.table.model.TableStatus
import com.example.restaurantops.table.repository.RestaurantTableRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrderService (
    private val storeService: StoreService,
    private val restaurantTableRepository: RestaurantTableRepository,
    private val orderRepository: OrderRepository,
) {
    @Transactional
    fun createOrder(
        storeId: UUID,
        request: CreateOrderRequest,
    ): OrderResponse {
        storeService.requireStore(storeId)

        val table = restaurantTableRepository.findByIdAndStoreId(
            tableId = request.tableId,
            storeId = storeId,
        ) ?: throw tableNotFound()

        if (orderRepository.existsOpenByTableId(table.id)) {
            throw openOrderConflict() // handles ordinary sequential requests clearly
        }

        val order = try {
            orderRepository.create(
                id = UUID.randomUUID(),
                storeId = storeId,
                tableId = table.id,
            )
        } catch (exception: DuplicateKeyException) {
            throw openOrderConflict() // handles the concurrency race enforced by uq_orders_active_table
        }

        restaurantTableRepository.updateStatus(
            storeId = storeId,
            tableId = table.id,
            status = TableStatus.SEATED,
        ) ?: throw tableNotFound()

        return OrderResponse.from(order)
    }
    
    fun getOrder(
        storeId: UUID,
        orderId: UUID,
    ): OrderResponse {
        storeService.requireStore(storeId)

        val order = orderRepository.findByIdAndStoreId(
            id = orderId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Order not found",
        )

        return OrderResponse.from(order)
    }
    
    private fun openOrderConflict(): ConflictException {
        return ConflictException(
            message = "Table already has an open order",
        )
    }
    
    private fun tableNotFound(): ResourceNotFoundException {
        return ResourceNotFoundException(
            message = "Table not found",
        )
    }
}