package com.example.restaurantops.menu

import com.example.restaurantops.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MenuItemOrderingIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val transactionManager: PlatformTransactionManager,
) : AbstractIntegrationTest() {

    @Test
    fun `swapping two item positions succeeds`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val itemA = createMenuItem(storeId, categoryId, "Item A", 100)
        val itemB = createMenuItem(storeId, categoryId, "Item B", 200)

        reorder(storeId, categoryId, listOf(itemB, itemA))
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(itemB) }
                jsonPath("$[0].displayOrder") { value(0) }
                jsonPath("$[1].id") { value(itemA) }
                jsonPath("$[1].displayOrder") { value(1) }
            }
    }

    @Test
    fun `duplicate final positions are rejected at commit`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val itemA = UUID.fromString(createMenuItem(storeId, categoryId, "Item A", 100))
        val itemB = UUID.fromString(createMenuItem(storeId, categoryId, "Item B", 200))

        val transactionTemplate = TransactionTemplate(transactionManager)

        assertThat(
            org.assertj.core.api.Assertions.catchThrowable {
                transactionTemplate.execute {
                    jdbcClient
                        .sql("SET CONSTRAINTS uq_menu_items_category_position DEFERRED")
                        .update()

                    jdbcClient
                        .sql("UPDATE menu_items SET display_order = 0 WHERE id = :id")
                        .param("id", itemA)
                        .update()

                    jdbcClient
                        .sql("UPDATE menu_items SET display_order = 0 WHERE id = :id")
                        .param("id", itemB)
                        .update()
                }
            },
        ).isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `an item from another store is rejected`() {
        val storeId = createStore()
        val anotherStoreId = createStore()
        val categoryId = createMenuCategory(storeId)
        val anotherCategoryId = createMenuCategory(anotherStoreId)

        val itemA = createMenuItem(storeId, categoryId, "Item A", 100)
        createMenuItem(storeId, categoryId, "Item B", 200)
        val foreignItem = createMenuItem(anotherStoreId, anotherCategoryId, "Foreign Item", 300)

        reorder(storeId, categoryId, listOf(itemA, foreignItem))
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("not_found") }
                jsonPath("$.error.message") { value("Menu item not found") }
            }
    }

    @Test
    fun `an incomplete menu item list is rejected`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val itemA = createMenuItem(storeId, categoryId, "Item A", 100)
        createMenuItem(storeId, categoryId, "Item B", 200)

        reorder(storeId, categoryId, listOf(itemA))
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("conflict") }
            }
    }

    @Test
    fun `two concurrent reorders of the same category serialize`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val itemA = createMenuItem(storeId, categoryId, "Item A", 100)
        val itemB = createMenuItem(storeId, categoryId, "Item B", 200)
        val itemC = createMenuItem(storeId, categoryId, "Item C", 300)

        val orderOne = listOf(itemC, itemB, itemA)
        val orderTwo = listOf(itemB, itemC, itemA)

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)

        try {
            val futures = listOf(orderOne, orderTwo).map { order ->
                executor.submit<Int> {
                    ready.countDown()
                    start.await()
                    reorder(storeId, categoryId, order)
                        .andReturn()
                        .response
                        .status
                }
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()

            val statuses = futures.map { it.get(10, TimeUnit.SECONDS) }
            assertThat(statuses).containsExactly(200, 200)
        } finally {
            executor.shutdownNow()
        }

        val finalOrder = jdbcClient
            .sql(
                "SELECT id, display_order FROM menu_items " +
                    "WHERE category_id = :categoryId ORDER BY display_order ASC",
            )
            .param("categoryId", UUID.fromString(categoryId))
            .query { rs, _ -> rs.getObject("id", UUID::class.java).toString() }
            .list()

        assertThat(finalOrder).isIn(orderOne, orderTwo)
    }

    @Test
    fun `two concurrent cross-category moves do not deadlock`() {
        val storeId = createStore()
        val categoryA = createMenuCategory(storeId)
        val categoryB = createMenuCategory(storeId)
        val itemX = createMenuItem(storeId, categoryA, "Item X", 100)
        val itemY = createMenuItem(storeId, categoryB, "Item Y", 200)

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)

        try {
            val futureX = executor.submit<Int> {
                ready.countDown()
                start.await()
                placement(storeId, itemX, categoryB, 0)
                    .andReturn()
                    .response
                    .status
            }
            val futureY = executor.submit<Int> {
                ready.countDown()
                start.await()
                placement(storeId, itemY, categoryA, 0)
                    .andReturn()
                    .response
                    .status
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()

            assertThat(futureX.get(10, TimeUnit.SECONDS)).isEqualTo(200)
            assertThat(futureY.get(10, TimeUnit.SECONDS)).isEqualTo(200)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `reversing the lock order deadlocks, which is why ordering matters`() {
        val storeId = createStore()
        val categoryA = UUID.fromString(createMenuCategory(storeId))
        val categoryB = UUID.fromString(createMenuCategory(storeId))

        val executor = Executors.newFixedThreadPool(2)
        val firstLockHeld = CountDownLatch(2)
        val proceedToSecondLock = CountDownLatch(1)
        val transactionTemplate = TransactionTemplate(transactionManager)

        fun lockCategory(categoryId: UUID) {
            jdbcClient
                .sql("SELECT id FROM menu_categories WHERE id = :id FOR UPDATE")
                .param("id", categoryId)
                .query(UUID::class.java)
                .single()
        }

        try {
            // Thread 1 locks A then B; thread 2 locks B then A -- the reverse
            // of the production code's always-ascending-UUID discipline. Each
            // holds its first lock and waits on the other's, so neither can
            // proceed: a genuine deadlock, not a race.
            val future1 = executor.submit {
                transactionTemplate.execute {
                    lockCategory(categoryA)
                    firstLockHeld.countDown()
                    proceedToSecondLock.await()
                    lockCategory(categoryB)
                }
            }
            val future2 = executor.submit {
                transactionTemplate.execute {
                    lockCategory(categoryB)
                    firstLockHeld.countDown()
                    proceedToSecondLock.await()
                    lockCategory(categoryA)
                }
            }

            assertThat(firstLockHeld.await(5, TimeUnit.SECONDS)).isTrue()
            proceedToSecondLock.countDown()

            val results = listOf(future1, future2).map { future ->
                runCatching { future.get(10, TimeUnit.SECONDS) }
            }

            assertThat(results.count { it.isFailure }).isEqualTo(1)

            val deadlockSqlState = results
                .first { it.isFailure }
                .exceptionOrNull()
                ?.let(::sqlStateOf)

            assertEquals("40P01", deadlockSqlState)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun sqlStateOf(throwable: Throwable): String? {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is SQLException && current.sqlState != null) {
                return current.sqlState
            }
            current = current.cause
        }
        return null
    }

    private fun reorder(
        storeId: String,
        categoryId: String,
        menuItemIds: List<String>,
    ): ResultActionsDsl {
        return mockMvc.put(
            "/api/stores/$storeId/menu-categories/$categoryId/menu-item-order",
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("menuItemIds" to menuItemIds),
            )
        }
    }

    private fun placement(
        storeId: String,
        menuItemId: String,
        categoryId: String,
        position: Int,
    ): ResultActionsDsl {
        return mockMvc.patch(
            "/api/stores/$storeId/menu-items/$menuItemId/placement",
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "categoryId" to categoryId,
                    "position" to position,
                ),
            )
        }
    }

    private fun createStore(): String {
        val result = mockMvc
            .post("/api/stores") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Ordering Test ${UUID.randomUUID()}",
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
            .post("/api/stores/$storeId/menu-categories") {
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
            }
            .andReturn()

        return objectMapper
            .readTree(result.response.contentAsString)
            .path("id")
            .asString()
    }
}
