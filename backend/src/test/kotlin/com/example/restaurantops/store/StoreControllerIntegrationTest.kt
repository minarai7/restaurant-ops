package com.example.restaurantops.store

import com.example.restaurantops.support.AbstractIntegrationTest
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class StoreControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) : AbstractIntegrationTest() {

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
                jsonPath("$.length()") { value(2) }
                // Ordered by created_at ASC, so the list is exactly these two.
                jsonPath("$[*].name") {
                    value(
                        contains(
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