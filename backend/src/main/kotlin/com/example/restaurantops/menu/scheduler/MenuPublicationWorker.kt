package com.example.restaurantops.menu.scheduler

import com.example.restaurantops.menu.repository.MenuPublicationScheduleRepository
import com.example.restaurantops.menu.service.MenuPublicationScheduleService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MenuPublicationWorker(
    private val menuPublicationScheduleRepository: MenuPublicationScheduleRepository,
    private val menuPublicationScheduleService: MenuPublicationScheduleService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Public and non-transactional so integration tests can invoke a single
     * tick directly, deterministically, instead of waiting on the schedule.
     *
     * Not wrapped in RetryingTransactionExecutor: every lock path in
     * MenuPublicationScheduleService acquires menu_items before a revision
     * row, and the worker only ever claims a schedule with SKIP LOCKED
     * (never blocks on one), so there is no cycle for 40P01 to arise from,
     * and nothing here runs at SERIALIZABLE isolation to raise 40001
     * either. Revisit if a future change adds a lock path that doesn't
     * follow that same order.
     */
    @Scheduled(
        fixedDelayString = "\${restaurant-ops.scheduling.publication-interval-ms:10000}",
    )
    fun runOnce() {
        val dueScheduleIds = menuPublicationScheduleRepository.findDueScheduleIds(BATCH_SIZE)

        for (scheduleId in dueScheduleIds) {
            try {
                menuPublicationScheduleService.processDueSchedule(scheduleId)
            } catch (exception: Exception) {
                // One unhealthy schedule must not abort the rest of the
                // batch -- log it and move on to the next row.
                logger.error(
                    "Failed to process publication schedule {}",
                    scheduleId,
                    exception,
                )
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 50
    }
}
