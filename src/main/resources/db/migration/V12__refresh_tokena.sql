CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                token_hash VARCHAR(64) NOT NULL UNIQUE,
                                user_id BIGINT NOT NULL REFERENCES users(id),
                                expires_at TIMESTAMP NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);