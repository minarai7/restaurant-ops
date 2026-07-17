package com.example.restaurantops.table

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
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
class RestaurantTableControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")
    }

    @Test
    fun `create table and list it`() {
        val storeId = createStore()

        val createResponse = mockMvc.post(
            "/api/stores/{storeId}/tables",
            storeId,
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "tableName": "T-01",
                  "seatCount": 4
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            header { exists("Location") }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.storeId") {
                value(storeId.toString())
            }
            jsonPath("$.tableName") { value("T-01") }
            jsonPath("$.seatCount") { value(4) }
            jsonPath("$.status") { value("EMPTY") }
            jsonPath("$.createdAt") { isNotEmpty() }
        }.andReturn()

        val tableId = objectMapper
            .readTree(createResponse.response.contentAsString)
            .path("id")
            .asString()

        mockMvc.get(
            "/api/stores/{storeId}/tables",
            storeId,
        ).andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].id") { value(tableId) }
            jsonPath("$[0].tableName") {
                value("T-01")
            }
            jsonPath("$[0].status") {
                value("EMPTY")
            }
        }
    }

    @Test
    fun `list tables only returns tables for selected store`() {
        val firstStoreId = createStore()
        val secondStoreId = createStore()

        createTable(
            storeId = firstStoreId,
            tableName = "T-01",
        )
        createTable(
            storeId = secondStoreId,
            tableName = "Other Store Table",
        )

        mockMvc.get(
            "/api/stores/{storeId}/tables",
            firstStoreId,
        ).andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].storeId") {
                value(firstStoreId.toString())
            }
            jsonPath("$[0].tableName") {
                value("T-01")
            }
        }
    }

    @Test
    fun `update table status`() {
        val storeId = createStore()
        val tableId = createTable(
            storeId = storeId,
            tableName = "T-01",
        )

        mockMvc.patch(
            "/api/stores/{storeId}/tables/{tableId}/status",
            storeId,
            tableId,
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "status": "SEATED"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") {
                value(tableId.toString())
            }
            jsonPath("$.status") {
                value("SEATED")
            }
        }

        mockMvc.get(
            "/api/stores/{storeId}/tables",
            storeId,
        ).andExpect {
            status { isOk() }
            jsonPath("$[0].status") {
                value("SEATED")
            }
        }
    }

    @Test
    fun `invalid table status returns 400`() {
        val storeId = createStore()
        val tableId = createTable(
            storeId = storeId,
            tableName = "T-01",
        )

        mockMvc.patch(
            "/api/stores/{storeId}/tables/{tableId}/status",
            storeId,
            tableId,
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "status": "BUSY"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") {
                value("invalid_input")
            }
            jsonPath("$.error.message") {
                value("Request body is invalid")
            }
        }
    }

    @Test
    fun `create table for missing store returns 404`() {
        val missingStoreId = UUID.randomUUID()

        mockMvc.post(
            "/api/stores/{storeId}/tables",
            missingStoreId,
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "tableName": "T-01",
                  "seatCount": 4
                }
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") {
                value("not_found")
            }
            jsonPath("$.error.message") {
                value("Store not found")
            }
        }
    }

    @Test
    fun `table from another store cannot be updated`() {
        val firstStoreId = createStore()
        val secondStoreId = createStore()

        val secondStoreTableId = createTable(
            storeId = secondStoreId,
            tableName = "T-01",
        )

        mockMvc.patch(
            "/api/stores/{storeId}/tables/{tableId}/status",
            firstStoreId,
            secondStoreTableId,
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "status": "SEATED"
                }
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error.code") {
                value("not_found")
            }
            jsonPath("$.error.message") {
                value("Table not found")
            }
        }
    }

    private fun createStore(): UUID {
        val response = mockMvc.post("/api/stores") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "Test Store ${UUID.randomUUID()}",
                ),
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        return UUID.fromString(
            objectMapper
                .readTree(response.response.contentAsString)
                .path("id")
                .asString(),
        )
    }

    private fun createTable(
        storeId: UUID,
        tableName: String,
        seatCount: Int = 4,
    ): UUID {
        val response = mockMvc.post(
            "/api/stores/{storeId}/tables",
            storeId,
        ) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf(
                    "tableName" to tableName,
                    "seatCount" to seatCount,
                ),
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        return UUID.fromString(
            objectMapper
                .readTree(response.response.contentAsString)
                .path("id")
                .asString(),
        )
    }
}