-- =========================================================
-- Restaurant tables
-- =========================================================

CREATE TABLE restaurant_tables (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    table_name VARCHAR(50) NOT NULL,
    seat_count INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'EMPTY',
    created_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_restaurant_tables_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE CASCADE,
    
    CONSTRAINT uq_restaurant_tables_store_name
        UNIQUE (store_id, table_name),
    
    CONSTRAINT uq_restaurant_tables_id_store
        UNIQUE (id, store_id),
    
    CONSTRAINT ck_restaurant_tables_name_not_blank
        CHECK (char_length(btrim(table_name)) > 0),
    
    CONSTRAINT ck_restaurant_tables_seat_count_positive
        CHECK (seat_count > 0),
    
    CONSTRAINT ck_restaurant_tables_status
        CHECK (
            status IN (
                'EMPTY',
                'SEATED',
                'CALLING',
                'CHECKOUT_WAITING',
                'CLOSED'
            )
        )
);


-- =========================================================
-- Menu categories
-- =========================================================

CREATE TABLE menu_categories (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_menu_categories_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_menu_categories_store_name
        UNIQUE (store_id, name),

    CONSTRAINT uq_menu_categories_id_store
        UNIQUE (id, store_id),

    CONSTRAINT ck_menu_categories_name_not_blank
        CHECK (char_length(btrim(name)) > 0),

    CONSTRAINT ck_menu_categories_display_order
        CHECK (display_order >= 0)
);


-- =========================================================
-- Menu items
-- =========================================================

CREATE TABLE menu_items (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    category_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price INTEGER NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_recommended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_menu_items_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_menu_items_category_store
        FOREIGN KEY (category_id, store_id)
        REFERENCES menu_categories (id, store_id),

    CONSTRAINT uq_menu_items_id_store
        UNIQUE (id, store_id),

    CONSTRAINT ck_menu_items_name_not_blank
        CHECK (char_length(btrim(name)) > 0),

    CONSTRAINT ck_menu_items_price_non_negative
        CHECK (price >= 0)
);


-- =========================================================
-- Customers
-- =========================================================

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    birthday DATE,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_customers_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_customers_id_store
        UNIQUE (id, store_id),

    CONSTRAINT ck_customers_display_name_not_blank
        CHECK (char_length(btrim(display_name)) > 0)
);


-- =========================================================
-- Orders
-- =========================================================

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    table_id UUID NOT NULL,
    customer_id UUID,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP NOT NULL,
    checked_out_at TIMESTAMP,

    CONSTRAINT fk_orders_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_orders_table_store
        FOREIGN KEY (table_id, store_id)
        REFERENCES restaurant_tables (id, store_id),

    CONSTRAINT fk_orders_customer_store
        FOREIGN KEY (customer_id, store_id)
        REFERENCES customers (id, store_id),

    CONSTRAINT uq_orders_id_store
        UNIQUE (id, store_id),

    CONSTRAINT ck_orders_status
        CHECK (
            status IN (
                'OPEN',
                'ORDERED',
                'CALLING_STAFF',
                'CHECKOUT_REQUESTED',
                'CHECKED_OUT',
                'CANCELLED'
            )
        )
);


-- =========================================================
-- Order items
-- =========================================================

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    menu_item_id UUID,
    item_name_snapshot VARCHAR(255) NOT NULL,
    unit_price_snapshot INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    note TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_items_menu_item
        FOREIGN KEY (menu_item_id)
        REFERENCES menu_items (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_order_items_name_not_blank
        CHECK (char_length(btrim(item_name_snapshot)) > 0),

    CONSTRAINT ck_order_items_price_non_negative
        CHECK (unit_price_snapshot >= 0),

    CONSTRAINT ck_order_items_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT ck_order_items_status
        CHECK (
            status IN (
                'ACTIVE',
                'CANCELLED'
            )
        )
);


-- =========================================================
-- Payments
-- =========================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    order_id UUID NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    subtotal INTEGER NOT NULL,
    tax INTEGER NOT NULL,
    total INTEGER NOT NULL,
    paid_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_payments_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_payments_order_store
        FOREIGN KEY (order_id, store_id)
        REFERENCES orders (id, store_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_payments_order
        UNIQUE (order_id),

    CONSTRAINT ck_payments_method
        CHECK (
            payment_method IN (
                'CASH',
                'CARD',
                'OTHER'
            )
        ),

    CONSTRAINT ck_payments_subtotal_non_negative
        CHECK (subtotal >= 0),

    CONSTRAINT ck_payments_tax_non_negative
        CHECK (tax >= 0),

    CONSTRAINT ck_payments_total_non_negative
        CHECK (total >= 0),

    CONSTRAINT ck_payments_total_matches
        CHECK (total = subtotal + tax)
);


-- =========================================================
-- Customer visits
-- =========================================================

CREATE TABLE customer_visits (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    order_id UUID NOT NULL,
    visited_at TIMESTAMP NOT NULL,
    total_spend INTEGER NOT NULL,

    CONSTRAINT fk_customer_visits_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_customer_visits_customer_store
        FOREIGN KEY (customer_id, store_id)
        REFERENCES customers (id, store_id),

    CONSTRAINT fk_customer_visits_order_store
        FOREIGN KEY (order_id, store_id)
        REFERENCES orders (id, store_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_customer_visits_order
        UNIQUE (order_id),

    CONSTRAINT ck_customer_visits_total_spend_non_negative
        CHECK (total_spend >= 0)
);


-- =========================================================
-- Idempotency keys
-- =========================================================

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_body JSONB,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_idempotency_keys_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_idempotency_keys_store_key
        UNIQUE (store_id, idempotency_key),

    CONSTRAINT ck_idempotency_keys_key_not_blank
        CHECK (char_length(btrim(idempotency_key)) > 0),

    CONSTRAINT ck_idempotency_keys_request_hash_not_blank
        CHECK (char_length(btrim(request_hash)) > 0),

    CONSTRAINT ck_idempotency_keys_status
        CHECK (
            status IN (
                'IN_PROGRESS',
                'COMPLETED',
                'FAILED'
            )
        )
);


-- =========================================================
-- Indexes
-- PostgreSQL does not automatically index foreign-key columns.
-- =========================================================

CREATE INDEX idx_restaurant_tables_store_status
    ON restaurant_tables (store_id, status);

CREATE INDEX idx_menu_categories_store_display_order
    ON menu_categories (store_id, display_order);

CREATE INDEX idx_menu_items_store_category
    ON menu_items (store_id, category_id);

CREATE INDEX idx_menu_items_store_available
    ON menu_items (store_id, is_available);

CREATE INDEX idx_customers_store
    ON customers (store_id);

CREATE INDEX idx_orders_store_status
    ON orders (store_id, status);

CREATE INDEX idx_orders_table
    ON orders (table_id);

CREATE INDEX idx_orders_customer
    ON orders (customer_id);

CREATE INDEX idx_order_items_order
    ON order_items (order_id);

CREATE INDEX idx_order_items_menu_item
    ON order_items (menu_item_id);

CREATE INDEX idx_payments_store_paid_at
    ON payments (store_id, paid_at);

CREATE INDEX idx_customer_visits_customer_visited_at
    ON customer_visits (store_id, customer_id, visited_at);

CREATE INDEX idx_idempotency_keys_store_status
    ON idempotency_keys (store_id, status);