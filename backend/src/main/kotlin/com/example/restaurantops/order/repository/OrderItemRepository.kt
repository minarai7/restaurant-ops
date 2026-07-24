package com.example.restaurantops.order.repository

import com.example.restaurantops.order.model.OrderItem
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class OrderItemRepository(
    private val jdbcClient: JdbcClient,
) {
    private val orderItemRowMapper = RowMapper<OrderItem> { rs, _ ->
        OrderItem(
            id = rs.getObject("id", UUID::class.java),
            orderId = rs.getObject("order_id", UUID::class.java),
            menuItemId = rs.getObject("menu_item_id", UUID::class.java),
            itemNameSnapshot = rs.getString("item_name_snapshot"),
            unitPriceSnapshot = rs.getInt("unit_price_snapshot"),
            quantity = rs.getInt("quantity"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
        )
    }

    fun add(
        id: UUID,
        orderId: UUID,
        menuItemId: UUID,
        itemNameSnapshot: String,
        unitPriceSnapshot: Int,
        quantity: Int,
    ): OrderItem {
        return jdbcClient.sql(
            """
            INSERT INTO order_items (
                id,
                order_id,
                menu_item_id,
                item_name_snapshot,
                unit_price_snapshot,
                quantity,
                created_at
            )
            VALUES (
                :id,
                :orderId,
                :menuItemId,
                :itemNameSnapshot,
                :unitPriceSnapshot,
                :quantity,
                CURRENT_TIMESTAMP
            )
            RETURNING
                id,
                order_id,
                menu_item_id,
                item_name_snapshot,
                unit_price_snapshot,
                quantity,
                created_at
            """.trimIndent(),
        )
            .param("id", id)
            .param("orderId", orderId)
            .param("menuItemId", menuItemId)
            .param("itemNameSnapshot", itemNameSnapshot)
            .param("unitPriceSnapshot", unitPriceSnapshot)
            .param("quantity", quantity)
            .query(orderItemRowMapper)
            .single()
    }

    fun findByIdAndOrderId(
        id: UUID,
        orderId: UUID,
    ): OrderItem? {
        return jdbcClient.sql(
            """
            SELECT
                id,
                order_id,
                menu_item_id,
                item_name_snapshot,
                unit_price_snapshot,
                quantity,
                created_at
            FROM order_items
            WHERE id = :id
              AND order_id = :orderId
            """.trimIndent(),
        )
            .param("id", id)
            .param("orderId", orderId)
            .query(orderItemRowMapper)
            .optional()
            .orElse(null)
    }

    fun updateQuantity(
        id: UUID,
        orderId: UUID,
        quantity: Int,
    ): OrderItem? {
        return jdbcClient.sql(
            """
            UPDATE order_items
            SET quantity = :quantity
            WHERE id = :id
              AND order_id = :orderId
            RETURNING
                id,
                order_id,
                menu_item_id,
                item_name_snapshot,
                unit_price_snapshot,
                quantity,
                created_at
            """.trimIndent(),
        )
            .param("quantity", quantity)
            .param("id", id)
            .param("orderId", orderId)
            .query(orderItemRowMapper)
            .optional()
            .orElse(null)
    }

    fun delete(
        id: UUID,
        orderId: UUID,
    ): Boolean {
        val updated = jdbcClient.sql(
            """
            DELETE FROM order_items
            WHERE id = :id
              AND order_id = :orderId
            """.trimIndent(),
        )
            .param("id", id)
            .param("orderId", orderId)
            .update()
        return updated > 0
    }
}
