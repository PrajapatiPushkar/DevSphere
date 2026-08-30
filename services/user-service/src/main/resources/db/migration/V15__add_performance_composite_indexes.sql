-- V15: Add targeted composite indexes for frequent query patterns, pagination, and sorting
CREATE INDEX idx_goals_user_status_created ON goals(user_id, status, created_at);
CREATE INDEX idx_tasks_user_status_created ON tasks(user_id, status, created_at);
CREATE INDEX idx_dsa_user_status_created ON dsa_problems(user_id, status, created_at);
CREATE INDEX idx_dev_projects_user_created ON developer_projects(user_id, created_at);
CREATE INDEX idx_experiences_user_start_date ON experiences(user_id, start_date);
