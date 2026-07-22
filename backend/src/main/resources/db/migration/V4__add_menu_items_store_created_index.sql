CREATE INDEX idx_menu_items_store_created
ON menu_items (
    store_id,
    created_at,
    id
);