package com.example.restaurantops.order.service

import com.example.restaurantops.common.error.ConflictException
import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.order.model.CheckoutRequest
import com.example.restaurantops.order.model.CheckoutResponse
import com.example.restaurantops.order.model.CreateOrderRequest
import com.example.restaurantops.order.model.OrderResponse
import com.example.restaurantops.order.model.OrderStatus
import com.example.restaurantops.order.repository.OrderItemRepository
import com.example.restaurantops.order.repository.OrderRepository
import com.example.restaurantops.payment.repository.PaymentRepository
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
    private val orderItemRepository: OrderItemRepository,
    private val paymentRepository: PaymentRepository,
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
    
    @Transactional
    fun checkout(
        storeId: UUID,
        orderId: UUID,
        request: CheckoutRequest,
    ): CheckoutResponse {
        val order = orderRepository.lockByIdAndStoreId(
            id = orderId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException("Order not found")

        if (order.status != OrderStatus.OPEN) {
            throw ConflictException(
                message = "Order is no longer open"
            )
        }

        val subtotal = orderItemRepository.sumTotalByOrderId(orderId)

        val payment = paymentRepository.insert(
            id = UUID.randomUUID(),
            storeId = storeId,
            orderId = orderId,
            paymentMethod = request.paymentMethod,
            subtotal = subtotal,
            tax = 0,
            total = subtotal,
        )

        val checkedOutOrder = orderRepository.updateToCheckedOut(orderId)

        restaurantTableRepository.updateStatus(
            storeId = storeId,
            tableId = order.tableId,
            status = TableStatus.CLOSED,
        )

        return CheckoutResponse.from(
            order = checkedOutOrder,
            payment = payment,
        )
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