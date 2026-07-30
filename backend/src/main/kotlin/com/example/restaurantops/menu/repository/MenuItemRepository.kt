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
                displayOrder = resultSet.getInt(
                    "display_order",
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
                    display_order,
                    created_at,
                    updated_at
                )
                VALUES (
                    :id,
                    :storeId,
                    :categoryId,
                    :isAvailable,
                    COALESCE(
                        (
                            SELECT MAX(display_order)
                            FROM menu_items
                            WHERE category_id = :categoryId
                        ),
                        -1
                    ) + 1,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING
                    id,
                    store_id,
                    category_id,
                    is_available,
                    display_order,
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
                    display_order,
                    created_at,
                    updated_at
                FROM menu_items
                WHERE store_id = :storeId
                ORDER BY
                    category_id ASC,
                    display_order ASC,
                    id ASC
                """.trimIndent(),
            )
            .param("storeId", storeId)
            .query(menuItemRowMapper)
            .list()
    }

    fun findAllByCategoryIdAndStoreId(
        categoryId: UUID,
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
                    display_order,
                    created_at,
                    updated_at
                FROM menu_items
                WHERE category_id = :categoryId
                  AND store_id = :storeId
                ORDER BY
                    display_order ASC,
                    id ASC
                """.trimIndent(),
            )
            .param("categoryId", categoryId)
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
                    display_order,
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
                    display_order,
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
                    display_order,
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

    /**
     * Defers the `(category_id, display_order)` uniqueness check to COMMIT for
     * the remainder of the current transaction. Must be called after the
     * relevant category row(s) are locked, and only inside a method already
     * running in a transaction — `SET CONSTRAINTS` applies to the transaction
     * on the current connection.
     */
    fun deferPositionConstraint() {
        jdbcClient
            .sql("SET CONSTRAINTS uq_menu_items_category_position DEFERRED")
            .update()
    }

    fun findIdsByCategoryIdAndStoreId(
        categoryId: UUID,
        storeId: UUID,
    ): List<UUID> {
        return jdbcClient
            .sql(
                """
                SELECT id
                FROM menu_items
                WHERE category_id = :categoryId
                  AND store_id = :storeId
                ORDER BY display_order ASC, id ASC
                """.trimIndent(),
            )
            .param("categoryId", categoryId)
            .param("storeId", storeId)
            .query{rs, _ -> rs.getObject("id", UUID::class.java)}
            .list()
    }

    /**
     * Renumbers a category's items to a dense, 0-based `display_order` matching
     * [menuItemIds]' position in the list. Relies on the caller having already
     * deferred [deferPositionConstraint] and locked the category, since the
     * intermediate positions this assigns can collide with each other.
     *
     * Returns the number of rows updated, which callers compare against
     * `menuItemIds.size` to detect a lost race (an id that no longer belongs
     * to this category/store).
     */
    fun applyOrder(
        categoryId: UUID,
        storeId: UUID,
        menuItemIds: List<UUID>,
    ): Int {
        return jdbcClient
            .sql(
                """
                UPDATE menu_items AS m
                SET
                    display_order = ordered.position - 1,
                    updated_at = CURRENT_TIMESTAMP
                FROM unnest(string_to_array(:menuItemIds, ',')::uuid[])
                     WITH ORDINALITY AS ordered(id, position)
                WHERE m.id = ordered.id
                  AND m.category_id = :categoryId
                  AND m.store_id = :storeId
                """.trimIndent(),
            )
            .param(
                "menuItemIds",
                menuItemIds.joinToString(","),
            )
            .param("categoryId", categoryId)
            .param("storeId", storeId)
            .update()
    }

    /**
     * Moves a menu item to a different category, parking it at [displayOrder].
     * Only legal while the position constraint is deferred, since the caller
     * is expected to immediately renumber both the source and destination
     * categories with [applyOrder].
     */
    fun moveToCategory(
        menuItemId: UUID,
        storeId: UUID,
        categoryId: UUID,
        displayOrder: Int,
    ): MenuItem? {
        return jdbcClient
            .sql(
                """
                UPDATE menu_items
                SET
                    category_id = :categoryId,
                    display_order = :displayOrder,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :menuItemId
                  AND store_id = :storeId
                RETURNING
                    id,
                    store_id,
                    category_id,
                    is_available,
                    display_order,
                    created_at,
                    updated_at
                """.trimIndent(),
            )
            .param("categoryId", categoryId)
            .param("displayOrder", displayOrder)
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .query(menuItemRowMapper)
            .optional()
            .orElse(null)
    }
}