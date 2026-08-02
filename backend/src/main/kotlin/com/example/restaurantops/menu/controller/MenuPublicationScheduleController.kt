package com.example.restaurantops.menu.controller

import com.example.restaurantops.menu.model.CreatePublicationScheduleRequest
import com.example.restaurantops.menu.model.MenuPublicationScheduleResponse
import com.example.restaurantops.menu.service.MenuPublicationScheduleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/stores/{storeId}")
class MenuPublicationScheduleController(
    private val menuPublicationScheduleService: MenuPublicationScheduleService,
) {

    @PostMapping("/menu-items/{menuItemId}/publication-schedules")
    fun create(
        @PathVariable storeId: UUID,
        @PathVariable menuItemId: UUID,
        @Valid @RequestBody request: CreatePublicationScheduleRequest,
    ): ResponseEntity<MenuPublicationScheduleResponse> {
        val response = menuPublicationScheduleService.create(
            storeId = storeId,
            menuItemId = menuItemId,
            request = request,
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .location(
                URI.create(
                    "/api/stores/$storeId/publication-schedules/${response.id}",
                ),
            )
            .body(response)
    }

    @GetMapping("/publication-schedules")
    fun list(
        @PathVariable storeId: UUID,
    ): List<MenuPublicationScheduleResponse> {
        return menuPublicationScheduleService.list(storeId = storeId)
    }

    @DeleteMapping("/publication-schedules/{scheduleId}")
    fun cancel(
        @PathVariable storeId: UUID,
        @PathVariable scheduleId: UUID,
    ): ResponseEntity<Void> {
        menuPublicationScheduleService.cancel(
            storeId = storeId,
            scheduleId = scheduleId,
        )
        return ResponseEntity.noContent().build()
    }
}
