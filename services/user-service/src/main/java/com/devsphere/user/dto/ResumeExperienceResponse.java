package com.devsphere.user.dto;

import com.devsphere.user.entity.ResumeExperience;
import java.time.Instant;

public class ResumeExperienceResponse {

    private Long id;
    private Long resumeProfileId;
    private Long experienceId;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public ResumeExperienceResponse() {
    }

    public ResumeExperienceResponse(ResumeExperience re) {
        this.id = re.getId();
        this.resumeProfileId = re.getResumeProfileId();
        this.experienceId = re.getExperienceId();
        this.displayOrder = re.getDisplayOrder();
        this.createdAt = re.getCreatedAt();
        this.updatedAt = re.getUpdatedAt();
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

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
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
