package com.example.restaurantops.order

import com.example.restaurantops.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OrderControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : AbstractIntegrationTest() {

    @Test
    fun `creates and fetches an order`() {
        val storeId = createStore()
        val tableId = createTable(storeId)

        val createResponse = mockMvc.post("/api/stores/$storeId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tableId" to tableId,
                ),
            )
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.storeId") { value(storeId.toString()) }
                jsonPath("$.tableId") { value(tableId.toString()) }
                jsonPath("$.status") { value("OPEN") }
                jsonPath("$.checkedOutAt") { doesNotExist() }
            }
            .andReturn()
            .response

        val orderId = UUID.fromString(
            objectMapper
                .readTree(createResponse.contentAsString)
                .path("id")
                .asString(),
        )

        mockMvc.get("/api/stores/$storeId/orders/$orderId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(orderId.toString()) }
                jsonPath("$.storeId") { value(storeId.toString()) }
                jsonPath("$.tableId") { value(tableId.toString()) }
                jsonPath("$.status") { value("OPEN") }
            }

        val tableStatus = jdbcClient.sql(
            """
            SELECT status
            FROM restaurant_tables
            WHERE id = :tableId
            """.trimIndent(),
        )
            .param("tableId", tableId)
            .query(String::class.java)
            .single()

        assertThat(tableStatus).isEqualTo("SEATED")
    }

    @Test
    fun `rejects a table belonging to another store`() {
        val selectedStoreId = createStore()
        val otherStoreId = createStore()
        val otherStoreTableId = createTable(otherStoreId)

        mockMvc.post("/api/stores/$selectedStoreId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tableId" to otherStoreTableId,
                ),
            )
        }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("not_found") }
                jsonPath("$.error.message") { value("Table not found") }
            }
    }

    @Test
    fun `rejects a second open order for the same table`() {
        val storeId = createStore()
        val tableId = createTable(storeId)

        createOrder(storeId, tableId)

        mockMvc.post("/api/stores/$storeId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tableId" to tableId,
                ),
            )
        }
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") {
                    value("conflict")
                }
                jsonPath("$.error.message") {
                    value("Table already has an open order")
                }
            }

        assertThat(countOpenOrders(tableId)).isEqualTo(1)
    }

    @Test
    fun `returns not found for a missing order`() {
        val storeId = createStore()
        val missingOrderId = UUID.randomUUID()

        mockMvc.get("/api/stores/$storeId/orders/$missingOrderId")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") { value("not_found") }
                jsonPath("$.error.message") { value("Order not found") }
            }
    }

    @Test
    fun `concurrent requests create only one open order`() {
        val storeId = createStore()
        val tableId = createTable(storeId)

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)

        try {
            val futures = (1..2).map {
                executor.submit<Int> {
                    ready.countDown()
                    start.await()

                    mockMvc.post("/api/stores/$storeId/orders") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(
                            mapOf(
                                "tableId" to tableId,
                            ),
                        )
                    }
                        .andReturn()
                        .response
                        .status
                }
            }

            assertThat(
                ready.await(5, TimeUnit.SECONDS),
            ).isTrue()

            start.countDown()

            val statuses = futures.map {
                it.get(10, TimeUnit.SECONDS)
            }

            assertThat(statuses)
                .containsExactlyInAnyOrder(201, 409)

            assertThat(countOpenOrders(tableId)).isEqualTo(1)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun createStore(): UUID {
        val response = mockMvc.post("/api/stores") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "Store-${UUID.randomUUID()}",
                ),
            )
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper
                .readTree(response.contentAsString)
                .path("id")
                .asString(),
        )
    }

    private fun createTable(storeId: UUID): UUID {
        val response = mockMvc.post("/api/stores/$storeId/tables") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tableName" to "T-${UUID.randomUUID()}",
                    "seatCount" to 4,
                ),
            )
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper
                .readTree(response.contentAsString)
                .path("id")
                .asString(),
        )
    }

    private fun createOrder(
        storeId: UUID,
        tableId: UUID,
    ): UUID {
        val response = mockMvc.post("/api/stores/$storeId/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tableId" to tableId,
                ),
            )
        }
            .andExpect {
                status { isCreated() }
            }
            .andReturn()
            .response

        return UUID.fromString(
            objectMapper
                .readTree(response.contentAsString)
                .path("id")
                .asString(),
        )
    }

    private fun countOpenOrders(tableId: UUID): Int {
        return jdbcClient.sql(
            """
            SELECT COUNT(*)
            FROM orders
            WHERE table_id = :tableId
              AND status = 'OPEN'
            """.trimIndent(),
        )
            .param("tableId", tableId)
            .query(Int::class.java)
            .single()
    }
}