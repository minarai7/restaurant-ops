CREATE UNIQUE INDEX uq_orders_active_table
ON orders (table_id)
WHERE status = 'OPEN';