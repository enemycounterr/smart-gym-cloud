CREATE INDEX IF NOT EXISTS idx_access_logs_timestamp_desc ON access_logs (time_stamp DESC);

CREATE INDEX IF NOT EXISTS idx_access_logs_client_id ON access_logs (client_id);

CREATE INDEX IF NOT EXISTS idx_access_logs_zone_id ON access_logs (access_zone_id);
