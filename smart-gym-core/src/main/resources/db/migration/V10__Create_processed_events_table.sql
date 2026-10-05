CREATE TABLE processed_events (
       event_id UUID PRIMARY KEY,
       event_type VARCHAR(100) NOT NULL,
       processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
   );

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);