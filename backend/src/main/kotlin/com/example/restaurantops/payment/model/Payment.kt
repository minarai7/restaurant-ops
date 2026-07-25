package com.example.restaurantops.payment.model

import java.time.LocalDateTime
import java.util.UUID

data class Payment(
    val id: UUID,
    val storeId: UUID,
    val orderId: UUID,
    val paymentMethod: String,
    val subtotal: Int,
    val tax: Int,
    val total: Int,
    val status: PaymentStatus,
    val paidAt: LocalDateTime,
)
