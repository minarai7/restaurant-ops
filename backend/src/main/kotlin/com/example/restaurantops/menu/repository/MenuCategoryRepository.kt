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
}