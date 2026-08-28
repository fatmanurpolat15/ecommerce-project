CREATE TABLE outbox_messages (
                                 id BIGSERIAL PRIMARY KEY,
                                 topic VARCHAR(255) NOT NULL,
                                 message_key VARCHAR(255) NOT NULL,
                                 payload TEXT NOT NULL,
                                 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                 created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                 sent_at TIMESTAMP
);