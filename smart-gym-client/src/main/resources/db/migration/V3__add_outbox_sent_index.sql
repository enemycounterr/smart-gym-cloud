CREATE INDEX idx_outbox_events_sent
    ON outbox_events (sent_at ASC)
    WHERE status = 'SENT';