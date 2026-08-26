CREATE TABLE career_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    professional_summary TEXT NULL,
    current_title VARCHAR(255) NULL,
    target_role VARCHAR(255) NULL,
    years_of_experience INT NULL,
    preferred_location VARCHAR(255) NULL,
    work_preference VARCHAR(50) NULL,
    availability VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_career_profiles_user_id UNIQUE (user_id)
);
