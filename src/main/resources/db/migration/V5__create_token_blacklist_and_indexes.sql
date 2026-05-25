CREATE TABLE token_blacklist (id BIGSERIAL PRIMARY KEY,token_jti VARCHAR(255) NOT NULL UNIQUE,expires_at TIMESTAMP NOT NULL);
CREATE INDEX idx_token_jti ON token_blacklist(token_jti);
