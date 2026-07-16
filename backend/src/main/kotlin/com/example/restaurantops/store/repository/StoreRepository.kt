package com.example.restaurantops.store.repository

import com.example.restaurantops.store.model.Store
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class StoreRepository (
    private val jdbcClient: JdbcClient,
) {
    private val storeRowMapper = RowMapper<Store> { resultSet, _ ->
        Store(
            id = resultSet.getObject("id", UUID::class.java),
            name = resultSet.getString("name"),
            createdAt = resultSet
                .getTimestamp("created_at")
                .toLocalDateTime(),
        )
    }
    
    fun create(id: UUID, name: String): Store {
        return jdbcClient
            .sql(
                """
                INSERT INTO stores (
                    id,
                    name,
                    created_at
                )
                VALUES (
                    :id,
                    :name,
                    CURRENT_TIMESTAMP
                )
                RETURNING
                    id,
                    name,
                    created_at
                """.trimIndent(),
                )
                .param("id", id)
                .param("name", name)
                .query(storeRowMapper)
                .single()
    }
    
    fun findAll(): List<Store> {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    name,
                    created_at
                FROM stores
                ORDER BY created_at ASC, id ASC
                """.trimIndent(),
            )
            .query(storeRowMapper)
            .list()
    }
    
    fun findById(storeId: UUID): Store? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    name,
                    created_at
                FROM stores
                WHERE id = :storeId
                """.trimIndent(),
            )
            .param("storeId", storeId)
            .query(storeRowMapper)
            .optional()
            .orElse(null)
    }
}