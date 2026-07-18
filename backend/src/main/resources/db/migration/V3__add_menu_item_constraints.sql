ALTER TABLE menu_items
ADD CONSTRAINT uq_menu_items_store_name
UNIQUE (store_id, name);