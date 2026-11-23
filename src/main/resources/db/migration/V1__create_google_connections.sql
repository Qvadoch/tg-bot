CREATE TABLE google_connections (
    telegram_user_id BIGINT PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT NOT NULL,
    access_token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
