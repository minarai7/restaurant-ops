package com.example.restaurantops.order.model

import com.example.restaurantops.payment.model.Payment
import java.util.UUID

data class CheckoutResponse(
    val orderId: UUID,
    val status: String,
    val checkedOutAt: String,
    val subtotal: Int,
    val tax: Int,
    val total: Int,
    val paymentMethod: String,
) {
    companion object {
        fun from(
            order: Order,
            payment: Payment,
        ): CheckoutResponse {
            return CheckoutResponse(
                orderId = order.id,
                status = order.status.name,
                checkedOutAt = order.checkedOutAt.toString(),
                subtotal = payment.subtotal,
                tax = payment.tax,
                total = payment.total,
                paymentMethod = payment.paymentMethod,
            )
        }
    }
}
