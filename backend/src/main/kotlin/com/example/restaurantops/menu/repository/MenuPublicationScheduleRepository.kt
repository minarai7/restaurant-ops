package com.example.restaurantops.menu.repository

import com.example.restaurantops.menu.model.MenuPublicationSchedule
import com.example.restaurantops.menu.model.MenuPublicationScheduleStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class MenuPublicationScheduleRepository(
    private val jdbcClient: JdbcClient,
) {
    private val rowMapper =
        RowMapper<MenuPublicationSchedule> { rs, _ ->
            MenuPublicationSchedule(
                id = rs.getObject("id", UUID::class.java),
                storeId = rs.getObject("store_id", UUID::class.java),
                menuItemId = rs.getObject("menu_item_id", UUID::class.java),
                revisionId = rs.getObject("revision_id", UUID::class.java),
                publishAt = rs.getTimestamp("publish_at").toLocalDateTime(),
                status =
                    MenuPublicationScheduleStatus.valueOf(
                        rs.getString("status"),
                    ),
                createdBy = rs.getObject("created_by", UUID::class.java),
                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                processedAt =
                    rs
                        .getTimestamp("processed_at")
                        ?.toLocalDateTime(),
            )
        }

    fun create(
        id: UUID,
        storeId: UUID,
        menuItemId: UUID,
        revisionId: UUID,
        publishAt: LocalDateTime,
        createdBy: UUID?,
    ): MenuPublicationSchedule {
        return jdbcClient
            .sql(
                """
                INSERT INTO menu_publication_schedules (
                    id,
                    store_id,
                    menu_item_id,
                    revision_id,
                    publish_at,
                    status,
                    created_by,
                    created_at
                )
                VALUES (
                    :id,
                    :storeId,
                    :menuItemId,
                    :revisionId,
                    :publishAt,
                    'SCHEDULED',
                    :createdBy,
                    CURRENT_TIMESTAMP
                )
                RETURNING
                    id,
                    store_id,
                    menu_item_id,
                    revision_id,
                    publish_at,
                    status,
                    created_by,
                    created_at,
                    processed_at
                """.trimIndent(),
            )
            .param("id", id)
            .param("storeId", storeId)
            .param("menuItemId", menuItemId)
            .param("revisionId", revisionId)
            .param("publishAt", publishAt)
            .param("createdBy", createdBy)
            .query(rowMapper)
            .single()
    }

    fun findAllByStoreId(
        storeId: UUID,
    ): List<MenuPublicationSchedule> {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    menu_item_id,
                    revision_id,
                    publish_at,
                    status,
                    created_by,
                    created_at,
                    processed_at
                FROM menu_publication_schedules
                WHERE store_id = :storeId
                ORDER BY publish_at ASC, id ASC
                """.trimIndent(),
            )
            .param("storeId", storeId)
            .query(rowMapper)
            .list()
    }

    fun lockByIdAndStoreId(
        id: UUID,
        storeId: UUID,
    ): MenuPublicationSchedule? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    menu_item_id,
                    revision_id,
                    publish_at,
                    status,
                    created_by,
                    created_at,
                    processed_at
                FROM menu_publication_schedules
                WHERE id = :id
                  AND store_id = :storeId
                FOR UPDATE
                """.trimIndent(),
            )
            .param("id", id)
            .param("storeId", storeId)
            .query(rowMapper)
            .optional()
            .orElse(null)
    }

    fun cancel(
        id: UUID,
    ): Int {
        return jdbcClient
            .sql(
                """
                UPDATE menu_publication_schedules
                SET status = 'CANCELLED'
                WHERE id = :id
                  AND status = 'SCHEDULED'
                """.trimIndent(),
            )
            .param("id", id)
            .update()
    }

    fun markProcessed(
        id: UUID,
    ): Int {
        return jdbcClient
            .sql(
                """
                UPDATE menu_publication_schedules
                SET status = 'PROCESSED',
                    processed_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """.trimIndent(),
            )
            .param("id", id)
            .update()
    }

    fun markFailed(
        id: UUID,
    ): Int {
        return jdbcClient
            .sql(
                """
                UPDATE menu_publication_schedules
                SET status = 'FAILED',
                    processed_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """.trimIndent(),
            )
            .param("id", id)
            .update()
    }

    /**
     * Candidate discovery for the worker's batch tick. Runs in its own
     * short-lived transaction, so SKIP LOCKED here only filters out rows
     * another instance is actively claiming right now -- the row lock is
     * released as soon as this query returns. Real mutual exclusion happens
     * per-row in [claimById].
     */
    fun findDueScheduleIds(
        limit: Int,
    ): List<UUID> {
        return jdbcClient
            .sql(
                """
                SELECT id
                FROM menu_publication_schedules
                WHERE status = 'SCHEDULED'
                  AND publish_at <= CURRENT_TIMESTAMP
                ORDER BY publish_at ASC, id ASC
                FOR UPDATE SKIP LOCKED
                LIMIT :limit
                """.trimIndent(),
            )
            .param("limit", limit)
            .query { rs, _ -> rs.getObject("id", UUID::class.java) }
            .list()
    }

    /**
     * The real claim, taken inside the per-row processing transaction. A
     * null result means another instance already holds this row -- the
     * caller should skip it rather than wait.
     */
    fun claimById(
        id: UUID,
    ): MenuPublicationSchedule? {
        return jdbcClient
            .sql(
                """
                SELECT
                    id,
                    store_id,
                    menu_item_id,
                    revision_id,
                    publish_at,
                    status,
                    created_by,
                    created_at,
                    processed_at
                FROM menu_publication_schedules
                WHERE id = :id
                  AND status = 'SCHEDULED'
                FOR UPDATE SKIP LOCKED
                """.trimIndent(),
            )
            .param("id", id)
            .query(rowMapper)
            .optional()
            .orElse(null)
    }
}