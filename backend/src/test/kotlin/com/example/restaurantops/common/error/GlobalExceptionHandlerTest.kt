package com.example.restaurantops.common.error

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(ErrorTestController::class)
@Import(GlobalExceptionHandler::class)
class GlobalExceptionHandlerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `validation failure returns standard error response`() {
        mockMvc.post("/test/errors/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "name": ""
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
    fun `missing resource returns standard error response`() {
        mockMvc.get("/test/errors/not-found")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error.code") {
                    value("not_found")
                }
                jsonPath("$.error.message") {
                    value("Store was not found")
                }
            }
    }

    @Test
    fun `business conflict returns standard error response`() {
        mockMvc.get("/test/errors/conflict")
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") {
                    value("conflict")
                }
                jsonPath("$.error.message") {
                    value("Store name already exists")
                }
            }
    }

    @Test
    fun `malformed JSON returns invalid input response`() {
        mockMvc.post("/test/errors/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "name":
                }
            """.trimIndent()
        }
            .andExpect {
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
    fun `unexpected error does not expose internal message`() {
        mockMvc.get("/test/errors/unexpected")
            .andExpect {
                status { isInternalServerError() }
                jsonPath("$.error.code") {
                    value("internal_error")
                }
                jsonPath("$.error.message") {
                    value("An unexpected server error occurred")
                }
            }
    }
}

@RestController
@RequestMapping("/test/errors")
class ErrorTestController {

    @PostMapping("/validation")
    fun validateRequest(
        @Valid @RequestBody request: ErrorTestRequest,
    ): ErrorTestRequest {
        return request
    }

    @GetMapping("/not-found")
    fun notFound(): Nothing {
        throw ResourceNotFoundException("Store was not found")
    }

    @GetMapping("/conflict")
    fun conflict(): Nothing {
        throw ConflictException("Store name already exists")
    }

    @GetMapping("/unexpected")
    fun unexpected(): Nothing {
        throw IllegalStateException(
            "Internal information that must not reach the client",
        )
    }
}

data class ErrorTestRequest(
    @field:NotBlank(message = "Name must not be blank")
    val name: String,
)