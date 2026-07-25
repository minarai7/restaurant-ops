package com.example.restaurantops.payment.repository

import com.example.restaurantops.payment.model.Payment
import com.example.restaurantops.payment.model.PaymentStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PaymentRepository(
    private val jdbcClient: JdbcClient,
) {
    private val paymentRowMapper = RowMapper<Payment> { resultSet, _ ->
        Payment(
            id = resultSet.getObject("id", UUID::class.java),
            storeId = resultSet.getObject("store_id", UUID::class.java),
            orderId = resultSet.getObject("order_id", UUID::class.java),
            paymentMethod = resultSet.getString("payment_method"),
            subtotal = resultSet.getInt("subtotal"),
            tax = resultSet.getInt("tax"),
            total = resultSet.getInt("total"),
            status = PaymentStatus.valueOf(resultSet.getString("status")),
            paidAt = resultSet
                .getTimestamp("paid_at")
                .toLocalDateTime(),
        )
    }

    fun insert(
        id: UUID,
        storeId: UUID,
        orderId: UUID,
        paymentMethod: String,
        subtotal: Int,
        tax: Int,
        total: Int,
    ): Payment {
        return jdbcClient.sql(
            """
            INSERT INTO payments (
                id,
                store_id,
                order_id,
                payment_method,
                subtotal,
                tax,
                total,
                status,
                paid_at
            )
            VALUES (
                :id,
                :storeId,
                :orderId,
                :paymentMethod,
                :subtotal,
                :tax,
                :total,
                :status,
                CURRENT_TIMESTAMP
            )
            RETURNING
                id,
                store_id,
                order_id,
                payment_method,
                subtotal,
                tax,
                total,
                status,
                paid_at
            """.trimIndent(),
        )
            .param("id", id)
            .param("storeId", storeId)
            .param("orderId", orderId)
            .param("paymentMethod", paymentMethod)
            .param("subtotal", subtotal)
            .param("tax", tax)
            .param("total", total)
            .param("status", PaymentStatus.SUCCEEDED.name)
            .query(paymentRowMapper)
            .single()
    }
}
