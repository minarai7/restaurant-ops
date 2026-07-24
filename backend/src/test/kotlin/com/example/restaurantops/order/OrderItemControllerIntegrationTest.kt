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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderItemControllerIntegrationTest @Autowired constructor(
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
    fun `adding an item creates an order-item row in the database`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Pad Thai", 1200)
        val orderId = createOrder(storeId, tableId)

        mockMvc.post("/api/stores/$storeId/orders/$orderId/items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "menuItemId" to menuItemId,
                    "quantity" to 2,
                ),
            )
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.orderId") { value(orderId.toString()) }
                jsonPath("$.menuItemId") { value(menuItemId.toString()) }
                jsonPath("$.itemNameSnapshot") { value("Pad Thai") }
                jsonPath("$.unitPriceSnapshot") { value(1200) }
                jsonPath("$.quantity") { value(2) }
                jsonPath("$.createdAt") { exists() }
            }

        assertThat(countOrderItems(orderId)).isEqualTo(1)
    }

    @Test
    fun `price and name snapshot are loaded from the menu item not from the client`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)
        val orderId = createOrder(storeId, tableId)

        val result = mockMvc.post("/api/stores/$storeId/orders/$orderId/items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "menuItemId" to menuItemId,
                    "quantity" to 1,
                ),
            )
        }
            .andExpect { status { isCreated() } }
            .andReturn()
            .response

        val body = objectMapper.readTree(result.contentAsString)
        assertThat(body.path("itemNameSnapshot").asString()).isEqualTo("Green Curry")
        assertThat(body.path("unitPriceSnapshot").asInt()).isEqualTo(950)
    }

    @Test
    fun `changing the menu item price after adding does not alter the snapshot`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Tom Yum", 800)
        val orderId = createOrder(storeId, tableId)

        val orderItemId = addItem(storeId, orderId, menuItemId, quantity = 1)

        jdbcClient.sql("UPDATE menu_items SET price = 9999 WHERE id = :id")
            .param("id", menuItemId)
            .update()

        val snapshot = jdbcClient.sql(
            "SELECT unit_price_snapshot FROM order_items WHERE id = :id",
        )
            .param("id", orderItemId)
            .query(Int::class.java)
            .single()

        assertThat(snapshot).isEqualTo(800)
    }

    @Test
    fun `updates item quantity`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Mango Salad", 700)
        val orderId = createOrder(storeId, tableId)
        val orderItemId = addItem(storeId, orderId, menuItemId, quantity = 1)

        mockMvc.patch("/api/stores/$storeId/orders/$orderId/items/$orderItemId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("quantity" to 3))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(orderItemId.toString()) }
                jsonPath("$.quantity") { value(3) }
                jsonPath("$.unitPriceSnapshot") { value(700) }
            }
    }

    @Test
    fun `deletes the item row from the database`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Spring Roll", 450)
        val orderId = createOrder(storeId, tableId)
        val orderItemId = addItem(storeId, orderId, menuItemId, quantity = 2)

        mockMvc.delete("/api/stores/$storeId/orders/$orderId/items/$orderItemId")
            .andExpect {
                status { isNoContent() }
            }

        assertThat(countOrderItems(orderId)).isEqualTo(0)
    }

    @Test
    fun `unavailable item is rejected`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Sold Out Soup", 600)
        val orderId = createOrder(storeId, tableId)
        
        jdbcClient.sql("UPDATE menu_items SET is_available = FALSE WHERE id = :id")
            .param("id", menuItemId)
            .update()

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
                jsonPath("$.error.message") { value("Menu item is not available") }
            }
    }

    @Test
    fun `item from another store is rejected`() {
        val storeA = createStore()
        val storeB = createStore()
        val tableId = createTable(storeA)
        val categoryB = createMenuCategory(storeB)
        val menuItemB = createMenuItem(storeB, categoryB, "Foreign Item", 500)
        val orderId = createOrder(storeA, tableId)

        mockMvc.post("/api/stores/$storeA/orders/$orderId/items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "menuItemId" to menuItemB,
                    "quantity" to 1,
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
    fun `checked-out order cannot have items added`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Lemongrass Chicken", 1100)
        val orderId = createOrder(storeId, tableId)

        setOrderStatus(orderId, "CHECKED_OUT")

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
    }

    @Test
    fun `checked-out order cannot have items updated`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Basil Stir Fry", 900)
        val orderId = createOrder(storeId, tableId)
        val orderItemId = addItem(storeId, orderId, menuItemId, quantity = 1)

        setOrderStatus(orderId, "CHECKED_OUT")

        mockMvc.patch("/api/stores/$storeId/orders/$orderId/items/$orderItemId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("quantity" to 5))
        }
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("conflict") }
                jsonPath("$.error.message") { value("Order is no longer open") }
            }
    }

    @Test
    fun `checked-out order cannot have items deleted`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Coconut Ice Cream", 350)
        val orderId = createOrder(storeId, tableId)
        val orderItemId = addItem(storeId, orderId, menuItemId, quantity = 1)

        setOrderStatus(orderId, "CHECKED_OUT")

        mockMvc.delete("/api/stores/$storeId/orders/$orderId/items/$orderItemId")
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("conflict") }
                jsonPath("$.error.message") { value("Order is no longer open") }
            }
    }

    @Test
    fun `zero quantity is rejected`() {
        val storeId = createStore()
        val tableId = createTable(storeId)
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Rice", 100)
        val orderId = createOrder(storeId, tableId)

        mockMvc.post("/api/stores/$storeId/orders/$orderId/items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "menuItemId" to menuItemId,
                    "quantity" to 0,
                ),
            )
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("invalid_input") }
                jsonPath("$.error.message") { value("Quantity must be greater than zero") }
            }
    }

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

    private fun setOrderStatus(orderId: UUID, status: String) {
        jdbcClient.sql("UPDATE orders SET status = :status WHERE id = :id")
            .param("status", status)
            .param("id", orderId)
            .update()
    }

    private fun countOrderItems(orderId: UUID): Int {
        return jdbcClient.sql(
            "SELECT COUNT(*) FROM order_items WHERE order_id = :orderId",
        )
            .param("orderId", orderId)
            .query(Int::class.java)
            .single()
    }
}
