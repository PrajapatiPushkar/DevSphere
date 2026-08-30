-- V16: Add public sharing lifecycle fields to resume_profiles
ALTER TABLE resume_profiles ADD COLUMN public_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE resume_profiles ADD COLUMN public_enabled_at TIMESTAMP NULL;

CREATE INDEX idx_resume_profiles_public_enabled ON resume_profiles (public_enabled);
