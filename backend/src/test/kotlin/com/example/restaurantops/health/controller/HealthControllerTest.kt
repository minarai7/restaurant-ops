package com.example.restaurantops.health.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest(
    @Autowired private val mockMvc: MockMvc
) {
    
    @Test
    fun `health endpoint returns ok`() {
        mockMvc.get("/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("ok") }
            }
    }
}