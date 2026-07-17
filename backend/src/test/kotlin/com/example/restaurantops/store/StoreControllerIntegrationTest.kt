package com.example.restaurantops.store

import org.hamcrest.Matchers.hasItems
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StoreControllerIntegrationTest @Autowired constructor(
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
    fun `create store and fetch it by id`() {
        val createResponse = mockMvc.post("/api/stores") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "name": "Shibuya Store"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            header { exists("Location") }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.name") { value("Shibuya Store") }
            jsonPath("$.createdAt") { isNotEmpty() }
        }.andReturn()
        
        val storeId = objectMapper
            .readTree(createResponse.response.contentAsString)
            .path("id")
            .asString()
        
        mockMvc.get("/api/stores/{storeId}", storeId)
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(storeId) }
                jsonPath("$.name") { value("Shibuya Store") }
            }
    }
    
    @Test
    fun `list stores returns created stores`() {
        createStore("Shinjuku Store")
        createStore("Ikebukuro Store")
        
        mockMvc.get("/api/stores")
            .andExpect {
                status { isOk() }
                jsonPath("$[*].name") {
                    value(
                        hasItems(
                            "Shinjuku Store",
                            "Ikebukuro Store",
                        ),
                    )
                }
            }
    }
    
    @Test
    fun `fetching missing store returns 404`() {
        val missingStoreId = UUID.randomUUID()

        mockMvc.get("/api/stores/{storeId}", missingStoreId)
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") {
                    value("not_found")
                }
            }
    }
    
    @Test
    fun `blank store name returns 400`() {
        mockMvc.post("/api/stores") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("name" to "   "),
            )
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") {
                value("invalid_input")
            }
        }
    }

    
    private fun createStore(name: String) {
        val result = mockMvc.post("/api/stores") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("name" to name),
            )
        }.andExpect {
            status { isCreated() }
        }.andReturn()
    }
}