CREATE TABLE dsa_problems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    goal_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    platform VARCHAR(50) NOT NULL,
    problem_url VARCHAR(512) NULL,
    difficulty VARCHAR(30) NOT NULL,
    topic VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    solved_at TIMESTAMP NULL,
    time_spent_minutes INT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dsa_user_id ON dsa_problems(user_id);
CREATE INDEX idx_dsa_user_status ON dsa_problems(user_id, status);
CREATE INDEX idx_dsa_user_difficulty ON dsa_problems(user_id, difficulty);
CREATE INDEX idx_dsa_user_topic ON dsa_problems(user_id, topic);
CREATE INDEX idx_dsa_user_platform ON dsa_problems(user_id, platform);
CREATE INDEX idx_dsa_user_solved_at ON dsa_problems(user_id, solved_at);
