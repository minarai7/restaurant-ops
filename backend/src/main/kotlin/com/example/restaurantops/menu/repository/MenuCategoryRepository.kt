package com.example.restaurantops.menu.repository

import com.example.restaurantops.menu.model.MenuCategory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MenuCategoryRepository (
    private val jdbcClient: JdbcClient
){
    private val menuCategoryRowMapper = RowMapper<MenuCategory> { resultSet, _ ->
        MenuCategory(
            id = resultSet.getObject("id", UUID::class.java),
            storeId = resultSet.getObject(
                "store_id", UUID::class.java
            ),
            name = resultSet.getString("name"),
            displayOrder = resultSet.getInt("display_order"),
            createdAt = resultSet
                .getTimestamp("created_at")
                .toLocalDateTime(),
        )    
    }
    
    fun create(id: UUID, storeId: UUID, name: String, displayOrder: Int): MenuCategory {
        return jdbcClient
            .sql(
                """
                INSERT INTO menu_categories (
                    id,
                    store_id,
                    name,
                    display_order,
                    created_at
                )
                VALUES (
                    :id,
                    :storeId,
                    :name,
                    :displayOrder,
                    CURRENT_TIMESTAMP
                )
                RETURNING
                    id,
                    store_id,
                    name,
                    display_order,
                    created_at
                """.trimIndent(),
            )
            .param("id", id)
            .param("storeId", storeId)
            .param("name", name)
            .param("displayOrder", displayOrder)
            .query(menuCategoryRowMapper)
            .single()
    }
    
    fun findAllByStoreId(storeId: UUID): List<MenuCategory> {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    name,
                    display_order,
                    created_at
                FROM menu_categories
                WHERE store_id = :storeId
                ORDER BY
                    display_order ASC,
                    created_at ASC,
                    id ASC
                """.trimIndent(),
            )
            .param("storeId", storeId)
            .query(menuCategoryRowMapper)
            .list()
    }
    
    fun findByIdAndStoreId(
        categoryId: UUID,
        storeId: UUID,
    ): MenuCategory? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    name,
                    display_order,
                    created_at
                FROM menu_categories
                WHERE id = :categoryId
                AND store_id = :storeId
                """.trimIndent(),
            )
            .param("categoryId", categoryId)
            .param("storeId", storeId)
            .query(menuCategoryRowMapper)
            .optional()
            .orElse(null)
    }

    /**
     * Locks a single category row so concurrent reorders/placements against it
     * serialize. Callers that need to lock more than one category (moving an
     * item between categories) must call this once per id, in ascending UUID
     * order, rather than locking several rows in one statement — Postgres does
     * not guarantee `FOR UPDATE` acquires locks in `ORDER BY` order.
     */
    fun lockByIdAndStoreId(
        categoryId: UUID,
        storeId: UUID,
    ): MenuCategory? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    name,
                    display_order,
                    created_at
                FROM menu_categories
                WHERE id = :categoryId
                AND store_id = :storeId
                FOR UPDATE
                """.trimIndent(),
            )
            .param("categoryId", categoryId)
            .param("storeId", storeId)
            .query(menuCategoryRowMapper)
            .optional()
            .orElse(null)
    }
}