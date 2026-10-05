create TABLE outbox_events (
       id UUID PRIMARY KEY,
       aggregate_type VARCHAR(50) NOT NULL,
       aggregate_id VARCHAR(50) NOT NULL,
       event_type VARCHAR(100) NOT NULL,
       routing_key VARCHAR(100) NOT NULL,
       payload TEXT NOT NULL,
       status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
       retry_count INT NOT NULL DEFAULT 0,
       error_message TEXT,
       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
       sent_at TIMESTAMP WITH TIME ZONE
   );

create index idx_outbox_events_pending
    on outbox_events (created_at asc)
    WHERE status = 'PENDING';