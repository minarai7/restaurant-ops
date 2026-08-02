package com.example.restaurantops.menu.model

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreatePublicationScheduleRequest(
    @field:NotNull(
        message = "Publish time is required",
    )
    @field:Future(
        message = "Publish time must be in the future",
    )
    val publishAt: LocalDateTime?,

    @field:NotNull(
        message = "Expected version is required",
    )
    val expectedVersion: Int?,
)