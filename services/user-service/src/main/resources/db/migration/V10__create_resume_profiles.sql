CREATE TABLE resume_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    target_role VARCHAR(255) NOT NULL,
    summary_override TEXT NULL,
    template VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resume_sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_profile_id BIGINT NOT NULL,
    section_type VARCHAR(50) NOT NULL,
    display_order INT NOT NULL DEFAULT 1,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resume_sections_type UNIQUE (resume_profile_id, section_type)
);

CREATE TABLE resume_experiences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_profile_id BIGINT NOT NULL,
    experience_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resume_experiences_exp UNIQUE (resume_profile_id, experience_id)
);

CREATE TABLE resume_educations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_profile_id BIGINT NOT NULL,
    education_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resume_educations_edu UNIQUE (resume_profile_id, education_id)
);

CREATE TABLE resume_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_profile_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resume_skills_skill UNIQUE (resume_profile_id, skill_id)
);

CREATE TABLE resume_certifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_profile_id BIGINT NOT NULL,
    certification_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resume_certifications_cert UNIQUE (resume_profile_id, certification_id)
);

CREATE TABLE resume_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_profile_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resume_projects_proj UNIQUE (resume_profile_id, project_id)
);

CREATE INDEX idx_resume_profiles_user ON resume_profiles(user_id);
CREATE INDEX idx_resume_sections_profile ON resume_sections(resume_profile_id);
CREATE INDEX idx_resume_experiences_profile ON resume_experiences(resume_profile_id);
CREATE INDEX idx_resume_educations_profile ON resume_educations(resume_profile_id);
CREATE INDEX idx_resume_skills_profile ON resume_skills(resume_profile_id);
CREATE INDEX idx_resume_certifications_profile ON resume_certifications(resume_profile_id);
CREATE INDEX idx_resume_projects_profile ON resume_projects(resume_profile_id);
