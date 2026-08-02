package com.example.restaurantops.menu.service

import com.example.restaurantops.common.error.ConflictException
import com.example.restaurantops.common.error.ResourceNotFoundException
import com.example.restaurantops.common.error.StaleVersionException
import com.example.restaurantops.menu.model.CreatePublicationScheduleRequest
import com.example.restaurantops.menu.model.MenuItemRevisionStatus
import com.example.restaurantops.menu.model.MenuPublicationScheduleResponse
import com.example.restaurantops.menu.model.MenuPublicationScheduleStatus
import com.example.restaurantops.menu.repository.MenuItemRepository
import com.example.restaurantops.menu.repository.MenuItemRevisionRepository
import com.example.restaurantops.menu.repository.MenuPublicationScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MenuPublicationScheduleService(
    private val menuItemRepository: MenuItemRepository,
    private val menuItemRevisionRepository: MenuItemRevisionRepository,
    private val menuPublicationScheduleRepository: MenuPublicationScheduleRepository,
) {

    @Transactional
    fun create(
        storeId: UUID,
        menuItemId: UUID,
        request: CreatePublicationScheduleRequest,
    ): MenuPublicationScheduleResponse {
        // Same lock order as MenuItemRevisionService.publish (menu_items,
        // then the draft revision) so scheduling can never deadlock against
        // an immediate publish running concurrently.
        menuItemRepository.lockByIdAndStoreId(
            id = menuItemId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item not found",
        )

        val draft = menuItemRevisionRepository.lockDraftByMenuItemIdAndStoreId(
            menuItemId = menuItemId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item has no draft",
        )

        if (draft.version != request.expectedVersion) {
            throw StaleVersionException(
                message = "Draft has been updated by another user",
            )
        }

        // Freeze the draft into an immutable, scheduled revision before
        // reseeding a new draft -- ux_menu_item_draft_revision allows only
        // one DRAFT per item, so the old one must vacate DRAFT first.
        val scheduled = menuItemRevisionRepository.markScheduled(draft.id)

        val schedule = menuPublicationScheduleRepository.create(
            id = UUID.randomUUID(),
            storeId = storeId,
            menuItemId = menuItemId,
            revisionId = scheduled.id,
            publishAt = requireNotNull(request.publishAt),
            createdBy = null,
        )

        menuItemRevisionRepository.createDraft(
            id = UUID.randomUUID(),
            menuItemId = menuItemId,
            storeId = storeId,
            name = scheduled.name,
            description = scheduled.description,
            price = scheduled.price,
            createdBy = scheduled.createdBy,
            version = scheduled.version + 1,
        )

        return MenuPublicationScheduleResponse.from(schedule)
    }

    fun list(
        storeId: UUID,
    ): List<MenuPublicationScheduleResponse> {
        return menuPublicationScheduleRepository
            .findAllByStoreId(storeId)
            .map(MenuPublicationScheduleResponse::from)
    }

    @Transactional
    fun cancel(
        storeId: UUID,
        scheduleId: UUID,
    ) {
        val schedule = menuPublicationScheduleRepository.lockByIdAndStoreId(
            id = scheduleId,
            storeId = storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Publication schedule not found",
        )

        when (schedule.status) {
            MenuPublicationScheduleStatus.CANCELLED -> return

            MenuPublicationScheduleStatus.SCHEDULED -> {
                // The frozen revision can otherwise never be published or
                // edited again -- archive it so it doesn't get stranded.
                menuItemRevisionRepository.archiveById(schedule.revisionId)
                menuPublicationScheduleRepository.cancel(scheduleId)
            }

            MenuPublicationScheduleStatus.PROCESSED,
            MenuPublicationScheduleStatus.FAILED,
            -> throw ConflictException(
                message = "Publication schedule is already finalized",
            )
        }
    }

    /**
     * Publishes one due schedule. Called by [com.example.restaurantops.menu.scheduler.MenuPublicationWorker]
     * with each row wrapped in its own transaction, so a claim lost to
     * another instance, or one poison row, never blocks the rest of the
     * batch.
     */
    @Transactional
    fun processDueSchedule(
        scheduleId: UUID,
    ) {
        // Null means another instance already claimed this row between the
        // worker's batch scan and this transaction starting -- nothing to
        // do here.
        val schedule = menuPublicationScheduleRepository.claimById(scheduleId)
            ?: return
        
        menuItemRepository.lockByIdAndStoreId(
            id = schedule.menuItemId,
            storeId = schedule.storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Menu item not found",
        )

        val revision = menuItemRevisionRepository.lockByIdAndStoreId(
            id = schedule.revisionId,
            storeId = schedule.storeId,
        ) ?: throw ResourceNotFoundException(
            message = "Scheduled revision not found",
        )
        
        when (revision.status) {
            MenuItemRevisionStatus.DRAFT,
            MenuItemRevisionStatus.ARCHIVED,
            -> menuPublicationScheduleRepository.markFailed(schedule.id)
            
            MenuItemRevisionStatus.PUBLISHED,
            -> menuPublicationScheduleRepository.markProcessed(schedule.id)
            
            MenuItemRevisionStatus.SCHEDULED -> {
                menuItemRevisionRepository.archivePublished(
                    menuItemId = revision.menuItemId,
                    storeId = revision.storeId,
                )
                
                menuItemRevisionRepository.publishScheduled(
                    id = revision.id
                )
                
                menuPublicationScheduleRepository.markProcessed(schedule.id)
            }
        }
    }
}