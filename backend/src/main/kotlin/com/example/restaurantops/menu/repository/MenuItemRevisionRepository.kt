package com.example.restaurantops.menu.repository

import com.example.restaurantops.menu.model.MenuItemRevision
import com.example.restaurantops.menu.model.MenuItemRevisionStatus
import com.example.restaurantops.menu.model.PublishedMenuItem
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MenuItemRevisionRepository(
    private val jdbcClient: JdbcClient,
) {
    private val rowMapper =
        RowMapper<MenuItemRevision> { rs, _ ->
            MenuItemRevision(
                id = rs.getObject("id", UUID::class.java),
                menuItemId = rs.getObject("menu_item_id", UUID::class.java),
                storeId = rs.getObject("store_id", UUID::class.java),
                revisionNumber = rs.getInt("revision_number"),
                status =
                    MenuItemRevisionStatus.valueOf(
                        rs.getString("status"),
                    ),
                name = rs.getString("name"),
                description = rs.getString("description"),
                price = rs.getInt("price"),
                version = rs.getInt("version"),
                createdBy =
                    rs.getObject(
                        "created_by",
                        UUID::class.java,
                    ),
                createdAt =
                    rs
                        .getTimestamp("created_at")
                        .toLocalDateTime(),
                publishedAt =
                    rs
                        .getTimestamp("published_at")
                        ?.toLocalDateTime(),
            )
        }

    private val publishedMenuItemRowMapper =
        RowMapper<PublishedMenuItem> { rs, _ ->
            PublishedMenuItem(
                id = rs.getObject("id", UUID::class.java),
                storeId = rs.getObject("store_id", UUID::class.java),
                categoryId = rs.getObject("category_id", UUID::class.java),
                revisionId = rs.getObject("revision_id", UUID::class.java),
                revisionNumber = rs.getInt("revision_number"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                price = rs.getInt("price"),
                isAvailable = rs.getBoolean("is_available"),
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                updatedAt = rs.getTimestamp("updated_at").toLocalDateTime(),
                publishedAt = rs.getTimestamp("published_at").toLocalDateTime(),
            )
        }

    fun createDraft(
        id: UUID,
        menuItemId: UUID,
        storeId: UUID,
        name: String,
        description: String?,
        price: Int,
        createdBy: UUID?,
        version: Int = 1,
    ): MenuItemRevision {
        return jdbcClient
            .sql(
                """
                INSERT INTO menu_item_revisions (
                    id,
                    menu_item_id,
                    store_id,
                    revision_number,
                    status,
                    name,
                    description,
                    price,
                    version,
                    created_by,
                    created_at
                )
                VALUES (
                    :id,
                    :menuItemId,
                    :storeId,
                    COALESCE(
                        (
                            SELECT MAX(revision_number)
                            FROM menu_item_revisions
                            WHERE menu_item_id = :menuItemId
                        ),
                        0
                    ) + 1,
                    'DRAFT',
                    :name,
                    :description,
                    :price,
                    :version,
                    :createdBy,
                    CURRENT_TIMESTAMP
                )
                RETURNING
                    id,
                    menu_item_id,
                    store_id,
                    revision_number,
                    status,
                    name,
                    description,
                    price,
                    version,
                    created_by,
                    created_at,
                    published_at
                """.trimIndent(),
            )
            .param("id", id)
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .param("name", name)
            .param("description", description)
            .param("price", price)
            .param("createdBy", createdBy)
            .param("version", version)
            .query(rowMapper)
            .single()
    }

    fun findAllByMenuItemIdAndStoreId(
        menuItemId: UUID,
        storeId: UUID,
    ): List<MenuItemRevision> {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    menu_item_id,
                    store_id,
                    revision_number,
                    status,
                    name,
                    description,
                    price,
                    version,
                    created_by,
                    created_at,
                    published_at
                FROM menu_item_revisions
                WHERE menu_item_id = :menuItemId
                  AND store_id = :storeId
                ORDER BY revision_number ASC
                """.trimIndent(),
            )
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .query(rowMapper)
            .list()
    }

    fun findDraftByMenuItemIdAndStoreId(
        menuItemId: UUID,
        storeId: UUID,
    ): MenuItemRevision? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    menu_item_id,
                    store_id,
                    revision_number,
                    status,
                    name,
                    description,
                    price,
                    version,
                    created_by,
                    created_at,
                    published_at
                FROM menu_item_revisions
                WHERE menu_item_id = :menuItemId
                  AND store_id = :storeId
                  AND status = 'DRAFT'
                ORDER BY revision_number DESC
                LIMIT 1
                """.trimIndent(),
            )
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .query(rowMapper)
            .optional()
            .orElse(null)
    }

    fun lockDraftByMenuItemIdAndStoreId(
        menuItemId: UUID,
        storeId: UUID,
    ): MenuItemRevision? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    menu_item_id,
                    store_id,
                    revision_number,
                    status,
                    name,
                    description,
                    price,
                    version,
                    created_by,
                    created_at,
                    published_at
                FROM menu_item_revisions
                WHERE menu_item_id = :menuItemId
                  AND store_id = :storeId
                  AND status = 'DRAFT'
                ORDER BY revision_number DESC
                LIMIT 1
                FOR UPDATE
                """.trimIndent(),
            )
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .query(rowMapper)
            .optional()
            .orElse(null)
    }

    fun archivePublished(
        menuItemId: UUID,
        storeId: UUID,
    ): Int {
        return jdbcClient
            .sql(
                """
                UPDATE menu_item_revisions
                SET status = 'ARCHIVED'
                WHERE menu_item_id = :menuItemId
                  AND store_id = :storeId
                  AND status = 'PUBLISHED'
                """.trimIndent(),
            )
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .update()
    }

    fun publishDraft(
        id: UUID,
    ): MenuItemRevision {
        return jdbcClient
            .sql(
                """
                UPDATE menu_item_revisions
                SET status = 'PUBLISHED',
                    published_at = CURRENT_TIMESTAMP
                WHERE id = :id
                RETURNING
                    id,
                    menu_item_id,
                    store_id,
                    revision_number,
                    status,
                    name,
                    description,
                    price,
                    version,
                    created_by,
                    created_at,
                    published_at
                """.trimIndent(),
            )
            .param("id", id)
            .query(rowMapper)
            .single()
    }

    fun updateDraft(
        storeId: UUID,
        menuItemId: UUID,
        name: String,
        description: String?,
        price: Int,
        expectedVersion: Int,
    ): MenuItemRevision? {
        return jdbcClient
            .sql(
                """
                UPDATE menu_item_revisions
                SET name = :name,
                    description = :description,
                    price = :price,
                    version = version + 1
                WHERE menu_item_id = :menuItemId
                  AND store_id = :storeId
                  AND status = 'DRAFT'
                  AND version = :expectedVersion
                RETURNING
                    id,
                    menu_item_id,
                    store_id,
                    revision_number,
                    status,
                    name,
                    description,
                    price,
                    version,
                    created_by,
                    created_at,
                    published_at
                """.trimIndent(),
            )
            .param("name", name)
            .param("description", description)
            .param("price", price)
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .param("expectedVersion", expectedVersion)
            .query(rowMapper)
            .optional()
            .orElse(null)
    }

    fun findPublishedByMenuItemIdAndStoreId(
        menuItemId: UUID,
        storeId: UUID,
    ): PublishedMenuItem? {
        return jdbcClient
            .sql(
            """
            SELECT
                mi.id,
                mi.store_id,
                mi.category_id,
                mir.id AS revision_id,
                mir.revision_number,
                mir.name,
                mir.description,
                mir.price,
                mi.is_available,
                mi.created_at,
                mi.updated_at,
                mir.published_at
            FROM menu_items mi
            JOIN menu_item_revisions mir
              ON mir.menu_item_id = mi.id
             AND mir.store_id = mi.store_id
             AND mir.status = :published
            WHERE mi.id = :menuItemId
             AND mi.store_id = :storeId
            """.trimIndent(),
            )
            .param("menuItemId", menuItemId)
            .param("storeId", storeId)
            .param("published", MenuItemRevisionStatus.PUBLISHED.name)
            .query(publishedMenuItemRowMapper)
            .optional()
            .orElse(null)
    }
}