package com.example.restaurantops.table.repository

import com.example.restaurantops.table.model.RestaurantTable
import com.example.restaurantops.table.model.TableStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RestaurantTableRepository (
    private val jdbcClient: JdbcClient,
) {
    private val tableRowMapper = RowMapper<RestaurantTable> { resultSet, _ ->
        RestaurantTable(
            id = resultSet.getObject(
                "id",
                UUID::class.java
            ),
            storeId = resultSet.getObject(
                "store_id",
                UUID::class.java
            ),
            tableName = resultSet.getString("table_name"),
            seatCount = resultSet.getInt("seat_count"),
            status = TableStatus.valueOf(
                resultSet.getString("status"),
            ),
            createdAt = resultSet
                .getTimestamp("created_at")
                .toLocalDateTime(),
        )
    }
    
    fun create(
        id: UUID,
        storeId: UUID,
        tableName: String,
        seatCount: Int,
        status: TableStatus,
    ): RestaurantTable {
        return jdbcClient
            .sql(
                """
                INSERT INTO restaurant_tables (
                    id,
                    store_id,
                    table_name,
                    seat_count,
                    status,
                    created_at
                )
                VALUES (
                    :id,
                    :storeId,
                    :tableName,
                    :seatCount,
                    :status,
                    CURRENT_TIMESTAMP
                )
                RETURNING
                    id,
                    store_id,
                    table_name,
                    seat_count,
                    status,
                    created_at
                """.trimIndent(),
            )
            .param("id", id)
            .param("storeId", storeId)
            .param("tableName", tableName)
            .param("seatCount", seatCount)
            .param("status", status.name)
            .query(tableRowMapper)
            .single()
    }
    
    fun findByIdAndStoreId(
        storeId: UUID,
        tableId: UUID,
    ): RestaurantTable? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    table_name,
                    seat_count,
                    status,
                    created_at
                FROM restaurant_tables
                WHERE id = :tableId
                AND store_id = :storeId
                """.trimIndent(),
            )
            .param("tableId", tableId)
            .param("storeId", storeId)
            .query(tableRowMapper)
            .optional()
            .orElse(null)
    }
    
    fun findAllByStoreId(
        storeId: UUID,
    ): List<RestaurantTable> {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    table_name,
                    seat_count,
                    status,
                    created_at
                FROM restaurant_tables
                WHERE store_id = :storeId
                ORDER BY table_name ASC, id ASC
                """.trimIndent(),
            )
            .param("storeId", storeId)
            .query(tableRowMapper)
            .list()
    }
    
    fun updateStatus(
        storeId: UUID,
        tableId: UUID,
        status: TableStatus,
    ): RestaurantTable? {
        return jdbcClient
            .sql(
                """
                UPDATE restaurant_tables
                SET status = :status
                WHERE id = :tableId
                  AND store_id = :storeId
                RETURNING
                    id,
                    store_id,
                    table_name,
                    seat_count,
                    status,
                    created_at
                """.trimIndent(),
            )
            .param("status", status.name)
            .param("tableId", tableId)
            .param("storeId", storeId)
            .query(tableRowMapper)
            .optional()
            .orElse(null)
    }
}