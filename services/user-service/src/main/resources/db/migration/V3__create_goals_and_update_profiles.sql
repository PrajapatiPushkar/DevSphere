ALTER TABLE user_profiles ADD COLUMN headline VARCHAR(250);
ALTER TABLE user_profiles ADD COLUMN location VARCHAR(100);
ALTER TABLE user_profiles ADD COLUMN github_url VARCHAR(255);
ALTER TABLE user_profiles ADD COLUMN linkedin_url VARCHAR(255);
ALTER TABLE user_profiles ADD COLUMN portfolio_url VARCHAR(255);
ALTER TABLE user_profiles ADD COLUMN `current_role` VARCHAR(100);
ALTER TABLE user_profiles ADD COLUMN years_of_experience INT;

CREATE TABLE goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    goal_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    target_value INT,
    current_value INT DEFAULT 0,
    target_date DATE,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_goals_user_id ON goals(user_id);
CREATE INDEX idx_goals_user_status ON goals(user_id, status);
CREATE INDEX idx_goals_user_type ON goals(user_id, goal_type);
