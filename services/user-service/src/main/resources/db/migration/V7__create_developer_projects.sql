CREATE TABLE developer_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(30) NOT NULL,
    project_type VARCHAR(50) NOT NULL,
    repository_url VARCHAR(512) NULL,
    live_url VARCHAR(512) NULL,
    documentation_url VARCHAR(512) NULL,
    tech_stack TEXT NULL,
    start_date DATE NULL,
    target_end_date DATE NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dev_projects_user_id ON developer_projects(user_id);
CREATE INDEX idx_dev_projects_user_status ON developer_projects(user_id, status);
CREATE INDEX idx_dev_projects_user_type ON developer_projects(user_id, project_type);
