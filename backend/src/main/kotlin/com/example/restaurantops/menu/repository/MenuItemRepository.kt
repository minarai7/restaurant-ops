package com.example.restaurantops.menu.repository

import com.example.restaurantops.menu.model.MenuItem
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MenuItemRepository(
    private val jdbcClient: JdbcClient,
) {

    private val menuItemRowMapper =
        RowMapper<MenuItem> { resultSet, _ ->
            MenuItem(
                id = resultSet.getObject(
                    "id",
                    UUID::class.java,
                ),
                storeId = resultSet.getObject(
                    "store_id",
                    UUID::class.java,
                ),
                categoryId = resultSet.getObject(
                    "category_id",
                    UUID::class.java,
                ),
                isAvailable = resultSet.getBoolean(
                    "is_available",
                ),
                createdAt = resultSet
                    .getTimestamp("created_at")
                    .toLocalDateTime(),
                updatedAt = resultSet
                    .getTimestamp("updated_at")
                    .toLocalDateTime(),
            )
        }

    fun create(
        id: UUID,
        storeId: UUID,
        categoryId: UUID,
        isAvailable: Boolean,
    ): MenuItem {
        return jdbcClient
            .sql(
                """
                INSERT INTO menu_items (
                    id,
                    store_id,
                    category_id,
                    is_available,
                    created_at,
                    updated_at
                )
                VALUES (
                    :id,
                    :storeId,
                    :categoryId,
                    :isAvailable,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING
                    id,
                    store_id,
                    category_id,
                    is_available,
                    created_at,
                    updated_at
                """.trimIndent(),
            )
            .param("id", id)
            .param("storeId", storeId)
            .param("categoryId", categoryId)
            .param("isAvailable", isAvailable)
            .query(menuItemRowMapper)
            .single()
    }

    fun findAllByStoreId(
        storeId: UUID,
    ): List<MenuItem> {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    category_id,
                    is_available,
                    created_at,
                    updated_at
                FROM menu_items
                WHERE store_id = :storeId
                ORDER BY
                    created_at ASC,
                    id ASC
                """.trimIndent(),
            )
            .param("storeId", storeId)
            .query(menuItemRowMapper)
            .list()
    }

    fun findByIdAndStoreId(
        id: UUID,
        storeId: UUID,
    ): MenuItem? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    category_id,
                    is_available,
                    created_at,
                    updated_at
                FROM menu_items
                WHERE id = :id
                  AND store_id = :storeId
                """.trimIndent(),
            )
            .param("id", id)
            .param("storeId", storeId)
            .query(menuItemRowMapper)
            .optional()
            .orElse(null)
    }

    fun lockByIdAndStoreId(
        id: UUID,
        storeId: UUID,
    ): MenuItem? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    category_id,
                    is_available,
                    created_at,
                    updated_at
                FROM menu_items
                WHERE id = :id
                  AND store_id = :storeId
                FOR UPDATE
                """.trimIndent(),
            )
            .param("id", id)
            .param("storeId", storeId)
            .query(menuItemRowMapper)
            .optional()
            .orElse(null)
    }

    fun updateAvailability(
        storeId: UUID,
        menuItemId: UUID,
        isAvailable: Boolean,
    ): MenuItem? {
        return jdbcClient
            .sql(
                """
                UPDATE menu_items
                SET
                    is_available = :isAvailable,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :menuItemId
                  AND store_id = :storeId
                RETURNING
                    id,
                    store_id,
                    category_id,
                    is_available,
                    created_at,
                    updated_at
                """.trimIndent(),
            )
            .param("isAvailable", isAvailable)
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .query(menuItemRowMapper)
            .optional()
            .orElse(null)
    }
}