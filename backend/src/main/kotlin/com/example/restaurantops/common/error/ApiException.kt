package com.example.restaurantops.common.error

import org.springframework.http.HttpStatus

open class ApiException(
    val status: HttpStatus,
    val code: String,
    message: String,
) : RuntimeException(message)

class ResourceNotFoundException(
    message: String,
) : ApiException(
    status = HttpStatus.NOT_FOUND,
    code = "not_found",
    message = message,
)

class ConflictException(
    message: String,
) : ApiException(
    status = HttpStatus.CONFLICT,
    code = "conflict",
    message = message,
)
