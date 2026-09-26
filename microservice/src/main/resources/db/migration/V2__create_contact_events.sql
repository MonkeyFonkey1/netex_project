CREATE TABLE activity.contact_events (
    event_id UUID PRIMARY KEY,
    contact_id BIGINT NOT NULL CHECK (contact_id > 0),
    actor_user_id BIGINT NOT NULL CHECK (actor_user_id > 0),
    action VARCHAR(7) NOT NULL CHECK (action IN ('CREATED', 'UPDATED', 'DELETED')),
    occurred_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
