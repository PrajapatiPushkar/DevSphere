package com.devsphere.user.dto;

import com.devsphere.user.entity.ResumeEducation;
import java.time.Instant;

public class ResumeEducationResponse {

    private Long id;
    private Long resumeProfileId;
    private Long educationId;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public ResumeEducationResponse() {
    }

    public ResumeEducationResponse(ResumeEducation re) {
        this.id = re.getId();
        this.resumeProfileId = re.getResumeProfileId();
        this.educationId = re.getEducationId();
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

    public Long getEducationId() {
        return educationId;
    }

    public void setEducationId(Long educationId) {
        this.educationId = educationId;
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
