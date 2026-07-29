package com.example.restaurantops.menu

import com.example.restaurantops.menu.model.PublishMenuItemRequest
import com.example.restaurantops.menu.service.MenuItemRevisionService
import com.example.restaurantops.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MenuItemControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val menuItemRevisionService: MenuItemRevisionService,
    private val transactionManager: PlatformTransactionManager,
) : AbstractIntegrationTest() {

    @Test
    fun `create menu item and list only selected store items`() {
        val firstStoreId = createStore()
        val secondStoreId = createStore()

        val firstCategoryId = createMenuCategory(firstStoreId)
        val secondCategoryId = createMenuCategory(secondStoreId)

        createMenuItem(
            storeId = firstStoreId,
            categoryId = firstCategoryId,
            name = "Chicken Curry",
            price = 1200,
        )

        createMenuItem(
            storeId = secondStoreId,
            categoryId = secondCategoryId,
            name = "Beef Curry",
            price = 1400,
        )

        val result = mockMvc
            .get("/api/stores/$firstStoreId/menu-items")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val menuItems = objectMapper.readTree(
            result.response.contentAsString,
        )

        assertEquals(1, menuItems.size())

        val menuItem = menuItems.path(0)

        assertEquals(
            firstStoreId,
            menuItem.path("storeId").asString(),
        )
        assertEquals(
            firstCategoryId,
            menuItem.path("categoryId").asString(),
        )
        assertEquals(
            true,
            menuItem.path("isAvailable").asBoolean(),
        )
    }

    @Test
    fun `creating a menu item also creates its first draft revision`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Green Curry",
            price = 950,
        )

        mockMvc
            .get("/api/stores/$storeId/menu-items/$menuItemId/draft")
            .andExpect {
                status { isOk() }
                jsonPath("$.menuItemId") { value(menuItemId) }
                jsonPath("$.revisionNumber") { value(1) }
                jsonPath("$.status") { value("DRAFT") }
                jsonPath("$.name") { value("Green Curry") }
                jsonPath("$.price") { value(950) }
            }

        mockMvc
            .get("/api/stores/$storeId/menu-items/$menuItemId/revisions")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].revisionNumber") { value(1) }
            }
    }

    @Test
    fun `draft for a menu item from another store is rejected`() {
        val storeId = createStore()
        val anotherStoreId = createStore()
        val categoryId = createMenuCategory(storeId)

        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Fried Rice",
            price = 850,
        )

        mockMvc
            .get("/api/stores/$anotherStoreId/menu-items/$menuItemId/draft")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("not_found") }
                jsonPath("$.error.message") { value("Menu item not found") }
            }
    }

    @Test
    fun `update draft bumps version and returns updated fields`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Spicy Chicken",
            price = 1200,
        )

        mockMvc
            .patch("/api/stores/$storeId/menu-items/$menuItemId/draft") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Spicy Chicken",
                        "description" to "Updated description",
                        "price" to 1350,
                        "expectedVersion" to 1,
                    ),
                )
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Spicy Chicken") }
                jsonPath("$.description") { value("Updated description") }
                jsonPath("$.price") { value(1350) }
                jsonPath("$.version") { value(2) }
            }
    }

    @Test
    fun `update draft with stale expectedVersion returns 409 stale_version`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Spicy Chicken",
            price = 1200,
        )

        mockMvc
            .patch("/api/stores/$storeId/menu-items/$menuItemId/draft") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Spicy Chicken",
                        "price" to 1300,
                        "expectedVersion" to 1,
                    ),
                )
            }
            .andExpect {
                status { isOk() }
            }

        mockMvc
            .patch("/api/stores/$storeId/menu-items/$menuItemId/draft") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Spicy Chicken",
                        "price" to 1400,
                        "expectedVersion" to 1,
                    ),
                )
            }
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("stale_version") }
            }
    }

    @Test
    fun `update draft for nonexistent menu item returns 404`() {
        val storeId = createStore()

        mockMvc
            .patch("/api/stores/$storeId/menu-items/${UUID.randomUUID()}/draft") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Spicy Chicken",
                        "price" to 1300,
                        "expectedVersion" to 1,
                    ),
                )
            }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("not_found") }
                jsonPath("$.error.message") { value("Menu item not found") }
            }
    }

    @Test
    fun `update draft with blank name is rejected`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Spicy Chicken",
            price = 1200,
        )

        mockMvc
            .patch("/api/stores/$storeId/menu-items/$menuItemId/draft") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "   ",
                        "price" to 1300,
                        "expectedVersion" to 1,
                    ),
                )
            }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("invalid_input") }
                jsonPath("$.error.message") { value("Name must not be blank") }
            }
    }

    @Test
    fun `update draft with negative price is rejected`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Spicy Chicken",
            price = 1200,
        )

        mockMvc
            .patch("/api/stores/$storeId/menu-items/$menuItemId/draft") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Spicy Chicken",
                        "price" to -1,
                        "expectedVersion" to 1,
                    ),
                )
            }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("invalid_input") }
                jsonPath("$.error.message") { value("Price must be zero or greater") }
            }
    }

    @Test
    fun `publishing a draft makes it published and opens a fresh draft`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Green Curry",
            price = 950,
        )

        publish(storeId, menuItemId, expectedVersion = 1)
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("PUBLISHED") }
                jsonPath("$.revisionNumber") { value(1) }
                jsonPath("$.version") { value(1) }
                jsonPath("$.publishedAt") { exists() }
            }

        mockMvc
            .get("/api/stores/$storeId/menu-items/$menuItemId/draft")
            .andExpect {
                status { isOk() }
                jsonPath("$.revisionNumber") { value(2) }
                jsonPath("$.version") { value(2) }
                jsonPath("$.status") { value("DRAFT") }
            }

        mockMvc
            .get("/api/stores/$storeId/menu-items/$menuItemId/revisions")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
            }
    }

    @Test
    fun `publishing again archives the previous published revision`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Spicy Chicken",
            price = 1200,
        )

        publish(storeId, menuItemId, expectedVersion = 1)
            .andExpect { status { isOk() } }

        mockMvc
            .patch("/api/stores/$storeId/menu-items/$menuItemId/draft") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Extra Spicy Chicken",
                        "price" to 1350,
                        "expectedVersion" to 2,
                    ),
                )
            }
            .andExpect { status { isOk() } }

        publish(storeId, menuItemId, expectedVersion = 3)
            .andExpect { status { isOk() } }

        val result = mockMvc
            .get("/api/stores/$storeId/menu-items/$menuItemId/revisions")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(3) }
            }
            .andReturn()

        val revisions = objectMapper.readTree(result.response.contentAsString)
        assertEquals("ARCHIVED", revisions.path(0).path("status").asString())
        assertEquals("PUBLISHED", revisions.path(1).path("status").asString())
        assertEquals("DRAFT", revisions.path(2).path("status").asString())
    }

    @Test
    fun `publishing with a stale expectedVersion returns 409 stale_version`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Spicy Chicken",
            price = 1200,
        )

        publish(storeId, menuItemId, expectedVersion = 1)
            .andExpect { status { isOk() } }

        publish(storeId, menuItemId, expectedVersion = 1)
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("stale_version") }
            }

        assertEquals(
            "PUBLISHED",
            publishedRevisionStatus(menuItemId),
        )
    }

    @Test
    fun `publish for a menu item from another store returns 404`() {
        val storeId = createStore()
        val anotherStoreId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Fried Rice",
            price = 850,
        )

        publish(anotherStoreId, menuItemId, expectedVersion = 1)
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("not_found") }
                jsonPath("$.error.message") { value("Menu item not found") }
            }
    }

    @Test
    fun `publish with no draft returns 404`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Fried Rice",
            price = 850,
        )

        jdbcClient.sql("DELETE FROM menu_item_revisions WHERE menu_item_id = :menuItemId")
            .param("menuItemId", UUID.fromString(menuItemId))
            .update()

        publish(storeId, menuItemId, expectedVersion = 1)
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("not_found") }
                jsonPath("$.error.message") { value("Menu item has no draft") }
            }
    }

    @Test
    fun `two concurrent publish requests publish only one revision`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Fried Rice",
            price = 850,
        )

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)

        try {
            val futures = (1..2).map {
                executor.submit<Int> {
                    ready.countDown()
                    start.await()
                    publish(storeId, menuItemId, expectedVersion = 1)
                        .andReturn()
                        .response
                        .status
                }
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()

            val statuses = futures.map { it.get(10, TimeUnit.SECONDS) }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409)
        } finally {
            executor.shutdownNow()
        }

        assertEquals(
            1,
            jdbcClient.sql(
                "SELECT COUNT(*) FROM menu_item_revisions " +
                    "WHERE menu_item_id = :menuItemId AND status = 'PUBLISHED'",
            )
                .param("menuItemId", UUID.fromString(menuItemId))
                .query(Int::class.java)
                .single(),
        )
        assertEquals(
            2,
            jdbcClient.sql(
                "SELECT COUNT(*) FROM menu_item_revisions WHERE menu_item_id = :menuItemId",
            )
                .param("menuItemId", UUID.fromString(menuItemId))
                .query(Int::class.java)
                .single(),
        )
    }

    @Test
    fun `a partially failed publish rolls back every state change`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Fried Rice",
            price = 850,
        )

        val transactionTemplate = TransactionTemplate(transactionManager)

        assertThatThrownBy {
            transactionTemplate.execute {
                menuItemRevisionService.publish(
                    storeId = UUID.fromString(storeId),
                    menuItemId = UUID.fromString(menuItemId),
                    request = PublishMenuItemRequest(expectedVersion = 1),
                )
                throw RuntimeException("simulated failure after publish")
            }
        }.isInstanceOf(RuntimeException::class.java)

        assertEquals(
            1,
            jdbcClient.sql(
                "SELECT COUNT(*) FROM menu_item_revisions WHERE menu_item_id = :menuItemId",
            )
                .param("menuItemId", UUID.fromString(menuItemId))
                .query(Int::class.java)
                .single(),
        )
        assertEquals(
            "DRAFT",
            jdbcClient.sql(
                "SELECT status FROM menu_item_revisions WHERE menu_item_id = :menuItemId",
            )
                .param("menuItemId", UUID.fromString(menuItemId))
                .query(String::class.java)
                .single(),
        )
    }

    @Test
    fun `negative menu item price is rejected`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        mockMvc
            .post("/api/stores/$storeId/menu-items") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "categoryId" to categoryId,
                        "name" to "Invalid Item",
                        "price" to -1,
                    ),
                )
            }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") {
                    value("invalid_input")
                }
                jsonPath("$.error.message") {
                    value("Price must be zero or greater")
                }
            }
    }

    @Test
    fun `blank menu item name is rejected`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        mockMvc
            .post("/api/stores/$storeId/menu-items") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "categoryId" to categoryId,
                        "name" to "   ",
                        "price" to 1000,
                    ),
                )
            }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") {
                    value("invalid_input")
                }
                jsonPath("$.error.message") {
                    value("Name must not be blank")
                }
            }
    }

    @Test
    fun `category from another store is rejected`() {
        val selectedStoreId = createStore()
        val anotherStoreId = createStore()

        val anotherStoreCategoryId =
            createMenuCategory(anotherStoreId)

        mockMvc
            .post(
                "/api/stores/$selectedStoreId/menu-items",
            ) {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "categoryId" to anotherStoreCategoryId,
                        "name" to "Cross-store Item",
                        "price" to 1000,
                    ),
                )
            }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") {
                    value("not_found")
                }
                jsonPath("$.error.message") {
                    value("Menu category not found")
                }
            }
    }

    @Test
    fun `update menu item availability`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        val menuItemId = createMenuItem(
            storeId = storeId,
            categoryId = categoryId,
            name = "Seasonal Dessert",
            price = 800,
        )

        mockMvc
            .patch(
                "/api/stores/$storeId/menu-items/" +
                    "$menuItemId/availability",
            ) {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "isAvailable" to false,
                    ),
                )
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") {
                    value(menuItemId)
                }
                jsonPath("$.isAvailable") {
                    value(false)
                }
            }

        mockMvc
            .get("/api/stores/$storeId/menu-items")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].isAvailable") {
                    value(false)
                }
            }
    }

    private fun createStore(): String {
        val result = mockMvc
            .post("/api/stores") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Menu Test ${UUID.randomUUID()}",
                    ),
                )
            }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        return objectMapper
            .readTree(result.response.contentAsString)
            .path("id")
            .asString()
    }

    private fun createMenuCategory(
        storeId: String,
    ): String {
        val result = mockMvc
            .post(
                "/api/stores/$storeId/menu-categories",
            ) {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Category ${UUID.randomUUID()}",
                        "displayOrder" to 0,
                    ),
                )
            }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()

        return objectMapper
            .readTree(result.response.contentAsString)
            .path("id")
            .asString()
    }

    private fun createMenuItem(
        storeId: String,
        categoryId: String,
        name: String,
        price: Int,
    ): String {
        val result = mockMvc
            .post("/api/stores/$storeId/menu-items") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "categoryId" to categoryId,
                        "name" to name,
                        "price" to price,
                    ),
                )
            }
            .andExpect {
                status { isCreated() }
                jsonPath("$.storeId") {
                    value(storeId)
                }
                jsonPath("$.categoryId") {
                    value(categoryId)
                }
                jsonPath("$.isAvailable") {
                    value(true)
                }
                jsonPath("$.id") {
                    exists()
                }
                jsonPath("$.createdAt") {
                    exists()
                }
                jsonPath("$.updatedAt") {
                    exists()
                }
            }
            .andReturn()

        return objectMapper
            .readTree(result.response.contentAsString)
            .path("id")
            .asString()
    }

    private fun publish(
        storeId: String,
        menuItemId: String,
        expectedVersion: Int,
    ) = mockMvc.post("/api/stores/$storeId/menu-items/$menuItemId/publish") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(
            mapOf(
                "expectedVersion" to expectedVersion,
            ),
        )
    }

    private fun publishedRevisionStatus(menuItemId: String): String {
        return jdbcClient.sql(
            """
            SELECT status
            FROM menu_item_revisions
            WHERE menu_item_id = :menuItemId
              AND status != 'DRAFT'
            ORDER BY revision_number DESC
            LIMIT 1
            """.trimIndent(),
        )
            .param("menuItemId", UUID.fromString(menuItemId))
            .query(String::class.java)
            .single()
    }
}