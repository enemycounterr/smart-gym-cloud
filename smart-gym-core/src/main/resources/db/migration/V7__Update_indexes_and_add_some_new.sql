--    UNIQUE constraint the column has already been indexed automatically
DROP INDEX IF EXISTS idx_access_logs_client_id;

DROP INDEX IF EXISTS idx_access_logs_timestamp_desc;

CREATE INDEX IF NOT EXISTS idx_access_logs_client_id_timestamp
    ON access_logs (client_id, time_stamp DESC);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);
