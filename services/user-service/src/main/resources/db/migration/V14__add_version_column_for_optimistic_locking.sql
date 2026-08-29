-- Migration V14: Add version column for optimistic locking across entities
ALTER TABLE user_profiles ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE goals ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE tasks ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE developer_projects ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE resume_profiles ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
