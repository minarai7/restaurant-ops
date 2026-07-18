/**
*AI generated and not edited yet.
**/
package com.example.restaurantops.menu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(
    classMode = DirtiesContext.ClassMode.AFTER_CLASS,
)
class MenuCategoryControllerIntegrationTest @Autowired constructor(
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
    fun `create categories and list them by display order`() {
        val storeId = createStore()

        createMenuCategory(
            storeId = storeId,
            name = "Desserts",
            displayOrder = 20,
        )

        createMenuCategory(
            storeId = storeId,
            name = "Starters",
            displayOrder = 10,
        )

        val result = mockMvc
            .get(
                "/api/stores/$storeId/menu-categories",
            )
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val categories = objectMapper.readTree(
            result.response.contentAsString,
        )

        assertEquals(2, categories.size())

        assertEquals(
            "Starters",
            categories.path(0).path("name").asString(),
        )
        assertEquals(
            10,
            categories.path(0).path("displayOrder").asInt(),
        )
        assertEquals(
            storeId,
            categories.path(0).path("storeId").asString(),
        )

        assertEquals(
            "Desserts",
            categories.path(1).path("name").asString(),
        )
        assertEquals(
            20,
            categories.path(1).path("displayOrder").asInt(),
        )
    }

    @Test
    fun `blank category name is rejected`() {
        val storeId = createStore()

        mockMvc
            .post(
                "/api/stores/$storeId/menu-categories",
            ) {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "name": "   ",
                      "displayOrder": 10
                    }
                """.trimIndent()
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
    fun `creating category for missing store returns not found`() {
        val missingStoreId = UUID.randomUUID()

        mockMvc
            .post(
                "/api/stores/$missingStoreId/menu-categories",
            ) {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "name": "Drinks",
                      "displayOrder": 10
                    }
                """.trimIndent()
            }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") {
                    value("not_found")
                }
                jsonPath("$.error.message") {
                    value("Store not found")
                }
            }
    }

    private fun createStore(): String {
        val result = mockMvc
            .post("/api/stores") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "name": "Menu Test ${UUID.randomUUID()}"
                    }
                """.trimIndent()
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
        name: String,
        displayOrder: Int,
    ) {
        mockMvc
            .post(
                "/api/stores/$storeId/menu-categories",
            ) {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "name": "$name",
                      "displayOrder": $displayOrder
                    }
                """.trimIndent()
            }
            .andExpect {
                status { isCreated() }
                jsonPath("$.storeId") {
                    value(storeId)
                }
                jsonPath("$.name") {
                    value(name)
                }
                jsonPath("$.displayOrder") {
                    value(displayOrder)
                }
                jsonPath("$.id") {
                    exists()
                }
                jsonPath("$.createdAt") {
                    exists()
                }
            }
    }
}