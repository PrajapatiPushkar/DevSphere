CREATE TABLE public_resume_view_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL,
    resume_profile_id BIGINT NOT NULL,
    accessed_at TIMESTAMP NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    referrer VARCHAR(512),
    user_agent VARCHAR(512),
    CONSTRAINT fk_prvl_resume_profile FOREIGN KEY (resume_profile_id) REFERENCES resume_profiles (id) ON DELETE CASCADE
);

CREATE INDEX idx_prvl_public_id ON public_resume_view_logs (public_id);
CREATE INDEX idx_prvl_profile_access ON public_resume_view_logs (resume_profile_id, accessed_at);
