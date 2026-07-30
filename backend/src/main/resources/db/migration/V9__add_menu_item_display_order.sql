ALTER TABLE menu_items
ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

-- Backfill dense, 0-based positions per category, preserving the order the
-- items were already listed in (created_at, id) so existing menus don't
-- visibly reshuffle the moment this migration runs.
UPDATE menu_items AS m
SET display_order = ordered.position
FROM (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY category_id
            ORDER BY created_at, id
        ) - 1 AS position
    FROM menu_items
) AS ordered
WHERE m.id = ordered.id;

ALTER TABLE menu_items
ADD CONSTRAINT ck_menu_items_display_order
CHECK (display_order >= 0);

-- DEFERRABLE INITIALLY IMMEDIATE: checked after every statement by default,
-- same as a plain UNIQUE constraint, but reordering transactions can defer
-- the check to COMMIT with `SET CONSTRAINTS ... DEFERRED` to swap positions
-- that would otherwise collide mid-transaction.
ALTER TABLE menu_items
ADD CONSTRAINT uq_menu_items_category_position
UNIQUE (category_id, display_order)
DEFERRABLE INITIALLY IMMEDIATE;
