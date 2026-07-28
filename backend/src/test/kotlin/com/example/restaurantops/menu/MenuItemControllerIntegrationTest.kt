package com.example.restaurantops.menu

import com.example.restaurantops.support.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class MenuItemControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
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
}