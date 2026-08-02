-- Widen the revision status check so a revision can be frozen for a future
-- schedule without being published or editable.
ALTER TABLE menu_item_revisions
DROP CONSTRAINT ck_menu_item_revisions_status;

ALTER TABLE menu_item_revisions
ADD CONSTRAINT ck_menu_item_revisions_status
CHECK (status IN ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'ARCHIVED'));

-- Composite unique target so a publication schedule's revision_id foreign key
-- can also pin menu_item_id and store_id, making it impossible to point a
-- schedule at a revision belonging to a different item or a different store.
ALTER TABLE menu_item_revisions
ADD CONSTRAINT uq_menu_item_revisions_id_item_store
UNIQUE (id, menu_item_id, store_id);

CREATE TABLE menu_publication_schedules (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    revision_id UUID NOT NULL,
    publish_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,

    CONSTRAINT fk_menu_publication_schedules_menu_item_store
        FOREIGN KEY (menu_item_id, store_id)
        REFERENCES menu_items (id, store_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_menu_publication_schedules_revision
        FOREIGN KEY (revision_id, menu_item_id, store_id)
        REFERENCES menu_item_revisions (id, menu_item_id, store_id)
        ON DELETE CASCADE,

    CONSTRAINT ck_menu_publication_schedules_status
        CHECK (status IN ('SCHEDULED', 'PROCESSED', 'CANCELLED', 'FAILED'))
);

-- Backs the worker's claim query: a partial index over only pending rows, so
-- the scan stays cheap as PROCESSED/CANCELLED/FAILED history accumulates.
CREATE INDEX idx_menu_publication_schedules_due
ON menu_publication_schedules (publish_at, id)
WHERE status = 'SCHEDULED';

CREATE INDEX idx_menu_publication_schedules_store
ON menu_publication_schedules (store_id, publish_at, id);
