package com.example.restaurantops.order.repository

import com.example.restaurantops.order.model.Order
import com.example.restaurantops.order.model.OrderStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class OrderRepository(
    private val jdbcClient: JdbcClient,
) {
    private val orderRowMapper = RowMapper<Order> { resultSet, _ ->
        Order(
            id = resultSet.getObject("id", UUID::class.java),
            storeId = resultSet.getObject("store_id", UUID::class.java),
            tableId = resultSet.getObject("table_id", UUID::class.java),
            status = OrderStatus.valueOf(resultSet.getString("status")),
            openedAt = resultSet
                .getTimestamp("opened_at")
                .toLocalDateTime(),
            checkedOutAt = resultSet
                .getTimestamp("checked_out_at")
                ?.toLocalDateTime(),
        )
    }
    
     fun create(
        id: UUID,
        storeId: UUID,
        tableId: UUID,
    ): Order {
        return jdbcClient.sql(
            """
            INSERT INTO orders (
                id,
                store_id,
                table_id,
                status,
                opened_at,
                checked_out_at
            )
            VALUES (
                :id,
                :storeId,
                :tableId,
                :status,
                CURRENT_TIMESTAMP,
                NULL
            )
            RETURNING
                id,
                store_id,
                table_id,
                status,
                opened_at,
                checked_out_at
            """.trimIndent(),
        )
            .param("id", id)
            .param("storeId", storeId)
            .param("tableId", tableId)
            .param("status", OrderStatus.OPEN.name)
            .query(orderRowMapper)
            .single()
    }

    fun findByIdAndStoreId(
        id: UUID,
        storeId: UUID,
    ): Order? {
        return jdbcClient.sql(
            """
            SELECT
                id,
                store_id,
                table_id,
                status,
                opened_at,
                checked_out_at
            FROM orders
            WHERE id = :id
              AND store_id = :storeId
            """.trimIndent(),
        )
            .param("id", id)
            .param("storeId", storeId)
            .query(orderRowMapper)
            .optional()
            .orElse(null)
    }

    fun existsOpenByTableId(tableId: UUID): Boolean {
        return jdbcClient.sql(
            """
            SELECT EXISTS (
                SELECT 1
                FROM orders
                WHERE table_id = :tableId
                  AND status = 'OPEN'
            ) AS has_open_order
            """.trimIndent(),
        )
            .param("tableId", tableId)
            .query { resultSet, _ ->
                resultSet.getBoolean("has_open_order")
            }
            .single()
    }
}