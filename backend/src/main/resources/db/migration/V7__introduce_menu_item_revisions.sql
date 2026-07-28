CREATE TABLE menu_item_revisions (
    id UUID PRIMARY KEY,
    menu_item_id UUID NOT NULL,
    store_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,

    CONSTRAINT fk_menu_item_revisions_menu_item_store
        FOREIGN KEY (menu_item_id, store_id)
        REFERENCES menu_items (id, store_id)
        ON DELETE CASCADE,

    CONSTRAINT ck_menu_item_revisions_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),

    CONSTRAINT ck_menu_item_revisions_price
        CHECK (price >= 0),

    CONSTRAINT ck_menu_item_revisions_version
        CHECK (version > 0),

    CONSTRAINT uq_menu_item_revisions_number
        UNIQUE (menu_item_id, revision_number)
);

INSERT INTO menu_item_revisions (
    id,
    menu_item_id,
    store_id,
    revision_number,
    status,
    name,
    description,
    price,
    version,
    created_by,
    created_at,
    published_at
)
SELECT
    gen_random_uuid(),
    id,
    store_id,
    1,
    'PUBLISHED',
    name,
    NULL,
    price,
    1,
    NULL,
    created_at,
    created_at
FROM menu_items;

ALTER TABLE menu_items
DROP CONSTRAINT IF EXISTS ck_menu_items_name_not_blank,
DROP CONSTRAINT IF EXISTS ck_menu_items_price_non_negative;

ALTER TABLE menu_items
DROP COLUMN name,
DROP COLUMN price,
DROP COLUMN description,
DROP COLUMN is_recommended;

CREATE INDEX idx_menu_item_revisions_item_status
ON menu_item_revisions (
    menu_item_id,
    status
);

CREATE INDEX idx_menu_item_revisions_store_status_created
ON menu_item_revisions (
    store_id,
    status,
    created_at,
    id
);