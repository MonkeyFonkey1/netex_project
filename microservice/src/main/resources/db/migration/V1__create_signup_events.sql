CREATE TABLE activity.signup_events (
    user_id BIGINT PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    signed_up_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_signup_events_email_not_blank CHECK (btrim(email) <> '')
);
