package com.example.restaurantops.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CheckoutControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jdbcClient: JdbcClient,
) {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")
    }

    @Test
    fun `total is calculated from order-item snapshots not the live menu price`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Ramen", 1000)
        val orderId = createOrder(storeId, tableId)
        addItem(storeId, orderId, menuItemId, quantity = 2)

        // Snapshot was captured at 1000; the later price change must be ignored.
        jdbcClient.sql("UPDATE menu_items SET price = 5000 WHERE id = :id")
            .param("id", menuItemId)
            .update()

        val response = checkout(storeId, orderId, "CASH")

        assertThat(response.status).isEqualTo(200)

        val body = objectMapper.readTree(response.contentAsString)
        assertThat(body.path("subtotal").asInt()).isEqualTo(2000)
        assertThat(body.path("total").asInt()).isEqualTo(2000)
        assertThat(body.path("tax").asInt()).isEqualTo(0)
        assertThat(body.path("status").asString()).isEqualTo("CHECKED_OUT")
    }

    @Test
    fun `checkout updates payment, order, and table atomically`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Gyoza", 600)
        val orderId = createOrder(storeId, tableId)
        addItem(storeId, orderId, menuItemId, quantity = 3)

        val response = checkout(storeId, orderId, "CARD")
        assertThat(response.status).isEqualTo(200)

        assertThat(countSucceededPayments(orderId)).isEqualTo(1)
        assertThat(orderStatus(orderId)).isEqualTo("CHECKED_OUT")
        assertThat(checkedOutAt(orderId)).isNotNull()
        assertThat(tableStatus(tableId)).isEqualTo("CLOSED")

        val paymentTotal = jdbcClient.sql(
            "SELECT total FROM payments WHERE order_id = :orderId",
        )
            .param("orderId", orderId)
            .query(Int::class.java)
            .single()
        assertThat(paymentTotal).isEqualTo(1800)
    }

    @Test
    fun `a failed payment insert rolls back the entire checkout`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Udon", 900)
        val orderId = createOrder(storeId, tableId)
        addItem(storeId, orderId, menuItemId, quantity = 1)

        // An unsupported payment method violates ck_payments_method, so the
        // payment INSERT fails after the order row is already locked. The whole
        // transaction must roll back and leave nothing changed.
        val response = checkout(storeId, orderId, "BITCOIN")
        assertThat(response.status).isEqualTo(409)

        assertThat(countSucceededPayments(orderId)).isEqualTo(0)
        assertThat(orderStatus(orderId)).isEqualTo("OPEN")
        assertThat(checkedOutAt(orderId)).isNull()
        assertThat(tableStatus(tableId)).isEqualTo("SEATED")
    }

    @Test
    fun `two concurrent checkouts produce only one successful payment`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Katsu", 1500)
        val orderId = createOrder(storeId, tableId)
        addItem(storeId, orderId, menuItemId, quantity = 1)

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)

        try {
            val futures = (1..2).map {
                executor.submit<Int> {
                    ready.countDown()
                    start.await()
                    checkout(storeId, orderId, "CASH").status
                }
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()

            val statuses = futures.map { it.get(10, TimeUnit.SECONDS) }

            assertThat(statuses).containsExactlyInAnyOrder(200, 409)
            assertThat(countSucceededPayments(orderId)).isEqualTo(1)
            assertThat(orderStatus(orderId)).isEqualTo("CHECKED_OUT")
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `an item cannot be added once checkout has completed`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Miso Soup", 300)
        val orderId = createOrder(storeId, tableId)
        addItem(storeId, orderId, menuItemId, quantity = 1)

        val checkoutResponse = checkout(storeId, orderId, "CASH")
        assertThat(checkoutResponse.status).isEqualTo(200)

        // The order row is now CHECKED_OUT; the add-item path locks the same row
        // and must reject the mutation with the shared "no longer open" conflict.
        mockMvc.post("/api/stores/$storeId/orders/$orderId/items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "menuItemId" to menuItemId,
                    "quantity" to 1,
                ),
            )
        }
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("conflict") }
                jsonPath("$.error.message") { value("Order is no longer open") }
            }

        assertThat(countOrderItems(orderId)).isEqualTo(1)
    }

    @Test
    fun `a checked-out order cannot be checked out again`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Tempura", 1200)
        val orderId = createOrder(storeId, tableId)
        addItem(storeId, orderId, menuItemId, quantity = 1)

        assertThat(checkout(storeId, orderId, "CASH").status).isEqualTo(200)

        mockMvc.post("/api/stores/$storeId/orders/$orderId/checkout") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("paymentMethod" to "CASH"))
        }
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("conflict") }
                jsonPath("$.error.message") { value("Order is no longer open") }
            }

        assertThat(countSucceededPayments(orderId)).isEqualTo(1)
    }

    private fun checkout(
        storeId: UUID,
        orderId: UUID,
        paymentMethod: String,
    ) = mockMvc.post("/api/stores/$storeId/orders/$orderId/checkout") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(
            mapOf("paymentMethod" to paymentMethod),
        )
    }
        .andReturn()
        .response

    private fun createStore(): UUID {
        val result = mockMvc.post("/api/stores") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("name" to "Store-${UUID.randomUUID()}"),
            )
        }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper.readTree(result.contentAsString).path("id").asString(),
        )
    }

    private fun createTable(storeId: UUID): UUID {
        val result = mockMvc.post("/api/stores/$storeId/tables") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tableName" to "T-${UUID.randomUUID()}",
                    "seatCount" to 4,
                ),
            )
        }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper.readTree(result.contentAsString).path("id").asString(),
        )
    }

    private fun createMenuCategory(storeId: UUID): UUID {
        val result = mockMvc.post("/api/stores/$storeId/menu-categories") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "Category-${UUID.randomUUID()}",
                    "displayOrder" to 0,
                ),
            )
        }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper.readTree(result.contentAsString).path("id").asString(),
        )
    }

    private fun createMenuItem(
        storeId: UUID,
        categoryId: UUID,
        name: String,
        price: Int,
    ): UUID {
        val result = mockMvc.post("/api/stores/$storeId/menu-items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "categoryId" to categoryId,
                    "name" to name,
                    "price" to price,
                ),
            )
        }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper.readTree(result.contentAsString).path("id").asString(),
        )
    }

    private fun createOrder(storeId: UUID, tableId: UUID): UUID {
        val result = mockMvc.post("/api/stores/$storeId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("tableId" to tableId),
            )
        }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper.readTree(result.contentAsString).path("id").asString(),
        )
    }

    private fun addItem(
        storeId: UUID,
        orderId: UUID,
        menuItemId: UUID,
        quantity: Int,
    ): UUID {
        val result = mockMvc.post("/api/stores/$storeId/orders/$orderId/items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "menuItemId" to menuItemId,
                    "quantity" to quantity,
                ),
            )
        }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper.readTree(result.contentAsString).path("id").asString(),
        )
    }

    private fun countSucceededPayments(orderId: UUID): Int {
        return jdbcClient.sql(
            """
            SELECT COUNT(*)
            FROM payments
            WHERE order_id = :orderId
              AND status = 'SUCCEEDED'
            """.trimIndent(),
        )
            .param("orderId", orderId)
            .query(Int::class.java)
            .single()
    }

    private fun countOrderItems(orderId: UUID): Int {
        return jdbcClient.sql(
            "SELECT COUNT(*) FROM order_items WHERE order_id = :orderId",
        )
            .param("orderId", orderId)
            .query(Int::class.java)
            .single()
    }

    private fun orderStatus(orderId: UUID): String {
        return jdbcClient.sql("SELECT status FROM orders WHERE id = :id")
            .param("id", orderId)
            .query(String::class.java)
            .single()
    }

    private fun checkedOutAt(orderId: UUID): Any? {
        return jdbcClient.sql("SELECT checked_out_at FROM orders WHERE id = :id")
            .param("id", orderId)
            .query(java.sql.Timestamp::class.java)
            .optional()
            .orElse(null)
    }

    private fun tableStatus(tableId: UUID): String {
        return jdbcClient.sql("SELECT status FROM restaurant_tables WHERE id = :id")
            .param("id", tableId)
            .query(String::class.java)
            .single()
    }
}
