package com.devsphere.user.dto;

import com.devsphere.user.entity.ResumeProject;
import java.time.Instant;

public class ResumeProjectResponse {

    private Long id;
    private Long resumeProfileId;
    private Long projectId;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public ResumeProjectResponse() {
    }

    public ResumeProjectResponse(ResumeProject rp) {
        this.id = rp.getId();
        this.resumeProfileId = rp.getResumeProfileId();
        this.projectId = rp.getProjectId();
        this.displayOrder = rp.getDisplayOrder();
        this.createdAt = rp.getCreatedAt();
        this.updatedAt = rp.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResumeProfileId() {
        return resumeProfileId;
    }

    public void setResumeProfileId(Long resumeProfileId) {
        this.resumeProfileId = resumeProfileId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
