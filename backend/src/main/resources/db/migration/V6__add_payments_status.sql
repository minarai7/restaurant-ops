ALTER TABLE payments
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'SUCCEEDED';

ALTER TABLE payments
    ADD CONSTRAINT ck_payments_status
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED'));

ALTER TABLE payments
    DROP CONSTRAINT uq_payments_order;

CREATE UNIQUE INDEX ux_payments_succeeded_order
    ON payments (order_id)
    WHERE status = 'SUCCEEDED';
