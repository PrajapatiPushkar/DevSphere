-- V12: Add database constraint guaranteeing at most one PUBLISHED resume version per profile
ALTER TABLE resume_versions
ADD COLUMN published_profile_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'PUBLISHED' THEN resume_profile_id ELSE NULL END);

CREATE UNIQUE INDEX uk_resume_versions_published_profile ON resume_versions (published_profile_id);
