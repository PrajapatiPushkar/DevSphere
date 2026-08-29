CREATE TABLE resume_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_profile_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    snapshot_data LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    archived_at TIMESTAMP NULL,
    CONSTRAINT uk_resume_versions_profile_ver UNIQUE (resume_profile_id, version_number),
    CONSTRAINT fk_resume_versions_profile FOREIGN KEY (resume_profile_id) REFERENCES resume_profiles(id) ON DELETE CASCADE
);

CREATE INDEX idx_resume_versions_profile ON resume_versions(resume_profile_id);
CREATE INDEX idx_resume_versions_user ON resume_versions(user_id);
CREATE INDEX idx_resume_versions_profile_status ON resume_versions(resume_profile_id, status);
