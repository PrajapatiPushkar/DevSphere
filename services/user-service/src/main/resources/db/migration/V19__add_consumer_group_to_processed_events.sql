-- Flyway migration for User Service Processed Events Table Enhancement
ALTER TABLE processed_events
    ADD COLUMN consumer_group VARCHAR(100) NOT NULL DEFAULT 'default';

ALTER TABLE processed_events
    DROP CONSTRAINT uk_processed_events_event_id;

ALTER TABLE processed_events
    ADD CONSTRAINT uk_processed_events_event_consumer UNIQUE (event_id, consumer_group);
