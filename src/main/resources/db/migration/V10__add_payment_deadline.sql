ALTER TABLE orders ADD COLUMN payment_deadline TIMESTAMP;

UPDATE orders SET payment_deadline = created_at + INTERVAL '2 minutes';

ALTER TABLE orders ALTER COLUMN payment_deadline SET NOT NULL;
