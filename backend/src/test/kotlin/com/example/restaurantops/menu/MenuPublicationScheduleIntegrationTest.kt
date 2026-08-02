package com.example.restaurantops.menu

import com.example.restaurantops.menu.scheduler.MenuPublicationWorker
import com.example.restaurantops.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MenuPublicationScheduleIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val menuPublicationWorker: MenuPublicationWorker,
) : AbstractIntegrationTest() {

    @Test
    fun `scheduling a draft freezes it and opens a new editable draft`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)
        val draftId = currentDraftId(menuItemId)

        createSchedule(storeId, menuItemId, LocalDateTime.now().plusDays(1), expectedVersion = 1)
            .andExpect {
                status { isCreated() }
                jsonPath("$.menuItemId") { value(menuItemId) }
                jsonPath("$.status") { value("SCHEDULED") }
                jsonPath("$.revisionId") { value(draftId) }
            }

        assertEquals("SCHEDULED", revisionStatus(draftId))

        mockMvc
            .get("/api/stores/$storeId/menu-items/$menuItemId/draft")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("DRAFT") }
                jsonPath("$.version") { value(2) }
            }

        mockMvc
            .get("/api/stores/$storeId/menu-items/$menuItemId/revisions")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
            }
    }

    @Test
    fun `schedule with a past publishAt is rejected`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)

        createSchedule(storeId, menuItemId, LocalDateTime.now().minusDays(1), expectedVersion = 1)
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error.code") { value("invalid_input") }
            }
    }

    @Test
    fun `schedule with a stale expectedVersion returns 409 stale_version`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)

        createSchedule(storeId, menuItemId, LocalDateTime.now().plusDays(1), expectedVersion = 2)
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("stale_version") }
            }
    }

    @Test
    fun `editing the draft after scheduling does not change the frozen revision`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)
        val scheduledRevisionId = currentDraftId(menuItemId)

        createSchedule(storeId, menuItemId, LocalDateTime.now().plusDays(1), expectedVersion = 1)
            .andExpect { status { isCreated() } }

        mockMvc
            .patch("/api/stores/$storeId/menu-items/$menuItemId/draft") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Green Curry Deluxe",
                        "price" to 1200,
                        "expectedVersion" to 2,
                    ),
                )
            }
            .andExpect { status { isOk() } }

        val frozenName = jdbcClient
            .sql("SELECT name FROM menu_item_revisions WHERE id = :id")
            .param("id", UUID.fromString(scheduledRevisionId))
            .query(String::class.java)
            .single()

        assertEquals("Green Curry", frozenName)
        assertEquals("SCHEDULED", revisionStatus(scheduledRevisionId))
    }

    @Test
    fun `worker publishes a due schedule and archives the previously published revision`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)

        val firstRevisionId = currentDraftId(menuItemId)
        mockMvc
            .post("/api/stores/$storeId/menu-items/$menuItemId/publish") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("expectedVersion" to 1))
            }
            .andExpect { status { isOk() } }

        val scheduledRevisionId = currentDraftId(menuItemId)
        val scheduleId = createScheduleAndReturnId(
            storeId,
            menuItemId,
            LocalDateTime.now().plusDays(1),
            expectedVersion = 2,
        )
        backdate(scheduleId)

        menuPublicationWorker.runOnce()

        assertEquals("ARCHIVED", revisionStatus(firstRevisionId))
        assertEquals("PUBLISHED", revisionStatus(scheduledRevisionId))
        assertEquals("PROCESSED", scheduleStatus(scheduleId))

        val processedAtIsSet = jdbcClient
            .sql("SELECT processed_at IS NOT NULL FROM menu_publication_schedules WHERE id = :id")
            .param("id", UUID.fromString(scheduleId))
            .query(Boolean::class.java)
            .single()
        assertThat(processedAtIsSet).isTrue()
    }

    @Test
    fun `worker does not process a schedule before its publish time`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)

        val scheduleId = createScheduleAndReturnId(
            storeId,
            menuItemId,
            LocalDateTime.now().plusDays(1),
            expectedVersion = 1,
        )

        menuPublicationWorker.runOnce()

        assertEquals("SCHEDULED", scheduleStatus(scheduleId))
    }

    @Test
    fun `two concurrent worker ticks process each due schedule exactly once`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)

        val scheduleIds = (1..4).map {
            val menuItemId = createMenuItem(storeId, categoryId, "Item ${UUID.randomUUID()}", 900)
            val scheduleId = createScheduleAndReturnId(
                storeId,
                menuItemId,
                LocalDateTime.now().plusDays(1),
                expectedVersion = 1,
            )
            backdate(scheduleId)
            scheduleId
        }

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)

        try {
            val futures = (1..2).map {
                executor.submit {
                    ready.countDown()
                    start.await()
                    menuPublicationWorker.runOnce()
                }
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        scheduleIds.forEach { scheduleId ->
            assertEquals("PROCESSED", scheduleStatus(scheduleId))
        }
    }

    @Test
    fun `a permanently superseded target is marked FAILED and not re-claimed`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)

        val scheduleId = createScheduleAndReturnId(
            storeId,
            menuItemId,
            LocalDateTime.now().plusDays(1),
            expectedVersion = 1,
        )
        val revisionId = scheduleRevisionId(scheduleId)

        // Simulate the target revision having been permanently superseded
        // by the time the worker gets to it.
        jdbcClient
            .sql("UPDATE menu_item_revisions SET status = 'ARCHIVED' WHERE id = :id")
            .param("id", UUID.fromString(revisionId))
            .update()
        backdate(scheduleId)

        menuPublicationWorker.runOnce()
        assertEquals("FAILED", scheduleStatus(scheduleId))

        // A second tick must not throw, and must not re-claim the row.
        menuPublicationWorker.runOnce()
        assertEquals("FAILED", scheduleStatus(scheduleId))
    }

    @Test
    fun `cancelling a scheduled publication archives its revision and is idempotent`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)

        val scheduleId = createScheduleAndReturnId(
            storeId,
            menuItemId,
            LocalDateTime.now().plusDays(1),
            expectedVersion = 1,
        )
        val revisionId = scheduleRevisionId(scheduleId)

        mockMvc
            .delete("/api/stores/$storeId/publication-schedules/$scheduleId")
            .andExpect { status { isNoContent() } }

        assertEquals("CANCELLED", scheduleStatus(scheduleId))
        assertEquals("ARCHIVED", revisionStatus(revisionId))

        mockMvc
            .delete("/api/stores/$storeId/publication-schedules/$scheduleId")
            .andExpect { status { isNoContent() } }
    }

    @Test
    fun `cancelling an already processed schedule returns 409`() {
        val storeId = createStore()
        val categoryId = createMenuCategory(storeId)
        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)

        val scheduleId = createScheduleAndReturnId(
            storeId,
            menuItemId,
            LocalDateTime.now().plusDays(1),
            expectedVersion = 1,
        )
        backdate(scheduleId)
        menuPublicationWorker.runOnce()
        assertEquals("PROCESSED", scheduleStatus(scheduleId))

        mockMvc
            .delete("/api/stores/$storeId/publication-schedules/$scheduleId")
            .andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("conflict") }
            }
    }

    @Test
    fun `listing publication schedules only returns the selected store's schedules`() {
        val storeId = createStore()
        val otherStoreId = createStore()
        val categoryId = createMenuCategory(storeId)
        val otherCategoryId = createMenuCategory(otherStoreId)

        val menuItemId = createMenuItem(storeId, categoryId, "Green Curry", 950)
        val otherMenuItemId = createMenuItem(otherStoreId, otherCategoryId, "Beef Curry", 1400)

        createScheduleAndReturnId(storeId, menuItemId, LocalDateTime.now().plusDays(1), expectedVersion = 1)
        createScheduleAndReturnId(otherStoreId, otherMenuItemId, LocalDateTime.now().plusDays(1), expectedVersion = 1)

        mockMvc
            .get("/api/stores/$storeId/publication-schedules")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].menuItemId") { value(menuItemId) }
            }
    }

    private fun createStore(): String {
        val result = mockMvc
            .post("/api/stores") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf("name" to "Schedule Test ${UUID.randomUUID()}"),
                )
            }
            .andExpect { status { isCreated() } }
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString).path("id").asString()
    }

    private fun createMenuCategory(storeId: String): String {
        val result = mockMvc
            .post("/api/stores/$storeId/menu-categories") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "Category ${UUID.randomUUID()}",
                        "displayOrder" to 0,
                    ),
                )
            }
            .andExpect { status { isCreated() } }
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString).path("id").asString()
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
            .andExpect { status { isCreated() } }
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString).path("id").asString()
    }

    private fun createSchedule(
        storeId: String,
        menuItemId: String,
        publishAt: LocalDateTime,
        expectedVersion: Int,
    ) = mockMvc.post("/api/stores/$storeId/menu-items/$menuItemId/publication-schedules") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(
            mapOf(
                "publishAt" to publishAt.toString(),
                "expectedVersion" to expectedVersion,
            ),
        )
    }

    private fun createScheduleAndReturnId(
        storeId: String,
        menuItemId: String,
        publishAt: LocalDateTime,
        expectedVersion: Int,
    ): String {
        val result = createSchedule(storeId, menuItemId, publishAt, expectedVersion)
            .andExpect { status { isCreated() } }
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString).path("id").asString()
    }

    private fun currentDraftId(menuItemId: String): String {
        return jdbcClient
            .sql("SELECT id FROM menu_item_revisions WHERE menu_item_id = :menuItemId AND status = 'DRAFT'")
            .param("menuItemId", UUID.fromString(menuItemId))
            .query(UUID::class.java)
            .single()
            .toString()
    }

    private fun scheduleRevisionId(scheduleId: String): String {
        return jdbcClient
            .sql("SELECT revision_id FROM menu_publication_schedules WHERE id = :id")
            .param("id", UUID.fromString(scheduleId))
            .query(UUID::class.java)
            .single()
            .toString()
    }

    private fun revisionStatus(revisionId: String): String {
        return jdbcClient
            .sql("SELECT status FROM menu_item_revisions WHERE id = :id")
            .param("id", UUID.fromString(revisionId))
            .query(String::class.java)
            .single()
    }

    private fun scheduleStatus(scheduleId: String): String {
        return jdbcClient
            .sql("SELECT status FROM menu_publication_schedules WHERE id = :id")
            .param("id", UUID.fromString(scheduleId))
            .query(String::class.java)
            .single()
    }

    private fun backdate(scheduleId: String) {
        jdbcClient
            .sql("UPDATE menu_publication_schedules SET publish_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = :id")
            .param("id", UUID.fromString(scheduleId))
            .update()
    }
}
