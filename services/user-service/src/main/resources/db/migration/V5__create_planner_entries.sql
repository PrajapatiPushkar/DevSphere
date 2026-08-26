CREATE TABLE planner_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    planned_date DATE NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    sort_order INT NOT NULL,
    planned_minutes INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_planner_user_task_date UNIQUE (user_id, task_id, planned_date)
);

CREATE INDEX idx_planner_entries_user_date ON planner_entries(user_id, planned_date);
CREATE INDEX idx_planner_entries_user_task ON planner_entries(user_id, task_id);
CREATE INDEX idx_planner_entries_user_date_order ON planner_entries(user_id, planned_date, sort_order);
