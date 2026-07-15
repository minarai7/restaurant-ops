package com.example.restaurantops.common.error

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
    ): ResponseEntity<ApiErrorResponse> {
        return createResponse(
            status = exception.status,
            code = exception.code,
            message = exception.message ?: "Request could not be completed",
        )
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException
    ): ResponseEntity<ApiErrorResponse> {
        val message = exception.bindingResult
            .fieldErrors
            .firstOrNull()
            ?.defaultMessage
            ?: "Request validation failed"
        
            return createResponse(
                status = HttpStatus.BAD_REQUEST,
                code = "invalid_input",
                message = message,
            )
    }
    
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(
        exception: HttpMessageNotReadableException
    ): ResponseEntity<ApiErrorResponse> {
        logger.debug("Unable to read request body", exception)
        
        return createResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "invalid_input",
            message = "Request body is invalid",
        )
    }
    
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        exception: DataIntegrityViolationException,
    ): ResponseEntity<ApiErrorResponse> {
        logger.warn("Database constraint violation", exception)

        return createResponse(
            status = HttpStatus.CONFLICT,
            code = "conflict",
            message = "Request conflicts with existing data",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        exception: Exception,
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected server error", exception)

        return createResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = "internal_error",
            message = "An unexpected server error occurred",
        )
    }    
    
    private fun createResponse(
        status: HttpStatus,
        code: String,
        message: String,
    ): ResponseEntity<ApiErrorResponse> {
        val body = ApiErrorResponse(
            error = ApiError(
                code = code,
                message = message,
            )
        )
        
        return ResponseEntity
            .status(status)
            .body(body)
    }
}