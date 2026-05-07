CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE vpa_handles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vpa VARCHAR(150) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_vpa_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE balances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_balance_user FOREIGN KEY (account_id) REFERENCES users(id)
);

CREATE TABLE transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_ref VARCHAR(255) NOT NULL,
    payer_vpa VARCHAR(150) NOT NULL,
    payee_vpa VARCHAR(150) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    note VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_txn FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_ledger_user FOREIGN KEY (account_id) REFERENCES users(id)
);

CREATE TABLE upi_pin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    pin_hash VARCHAR(255) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    CONSTRAINT fk_upi_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE collect_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_vpa VARCHAR(150) NOT NULL,
    to_vpa VARCHAR(150) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    note VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE idempotency_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    endpoint VARCHAR(120) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_payload TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_idem UNIQUE (user_id, idempotency_key, endpoint),
    CONSTRAINT fk_idem_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(100) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
