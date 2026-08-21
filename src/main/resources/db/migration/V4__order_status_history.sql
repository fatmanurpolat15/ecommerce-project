CREATE TABLE order_status_history (
                                      id BIGSERIAL PRIMARY KEY,
                                      order_id BIGINT NOT NULL REFERENCES orders(id),
                                      previous_status VARCHAR(255),
                                      new_status VARCHAR(255) NOT NULL,
                                      changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id);