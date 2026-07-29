CREATE UNIQUE INDEX ux_menu_item_published_revision
ON menu_item_revisions (menu_item_id)
WHERE status = 'PUBLISHED';

CREATE UNIQUE INDEX ux_menu_item_draft_revision
ON menu_item_revisions (menu_item_id)
WHERE status = 'DRAFT';
