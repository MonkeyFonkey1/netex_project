CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_email_trimmed CHECK (email = btrim(email) AND email <> ''),
    CONSTRAINT ck_users_password_hash_not_blank CHECK (btrim(password_hash) <> '')
);

-- The database also prevents duplicate emails that differ only in letter case.
CREATE UNIQUE INDEX uk_users_email ON users (lower(email));

CREATE TABLE contacts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    -- Nullable until the photo upload feature is introduced in step 6.
    picture_path VARCHAR(255),
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_contacts_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_contacts_address_valid CHECK (btrim(address) <> '' AND char_length(address) <= 1000),
    CONSTRAINT ck_contacts_picture_path_not_blank CHECK (picture_path IS NULL OR btrim(picture_path) <> ''),
    CONSTRAINT fk_contacts_author FOREIGN KEY (created_by_user_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

-- Supports lookup of an author's contacts and the foreign-key deletion check.
CREATE INDEX idx_contacts_author ON contacts (created_by_user_id);
