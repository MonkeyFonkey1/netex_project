CREATE TABLE event_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    destination VARCHAR(24) NOT NULL,
    event_key VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_event_outbox_destination CHECK (destination IN ('KAFKA_SIGNUP', 'HTTP_CONTACT'))
);

CREATE INDEX idx_event_outbox_due ON event_outbox (next_attempt_at, id);
