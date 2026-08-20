-- V3: Composite indexes for high-traffic queries


CREATE INDEX idx_products_category_deleted ON products(category_id, deleted);

CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

CREATE INDEX idx_user_addresses_user_deleted ON user_addresses(user_id, deleted);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
