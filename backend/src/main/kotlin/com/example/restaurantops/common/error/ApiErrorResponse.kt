package com.example.restaurantops.common.error

data class ApiErrorResponse(
    val error: ApiError,
)

data class ApiError(
    val code: String,
    val message: String,
)