-- V13: Add public_id column to resume_profiles for opaque public sharing
ALTER TABLE resume_profiles ADD COLUMN public_id VARCHAR(36);

CREATE UNIQUE INDEX uk_resume_profiles_public_id ON resume_profiles (public_id);
