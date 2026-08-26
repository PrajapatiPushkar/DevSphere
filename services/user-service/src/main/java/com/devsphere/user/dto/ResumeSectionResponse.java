package com.devsphere.user.dto;

import com.devsphere.user.entity.ResumeSection;
import com.devsphere.user.entity.ResumeSectionType;
import java.time.Instant;

public class ResumeSectionResponse {

    private Long id;
    private Long resumeProfileId;
    private ResumeSectionType sectionType;
    private Integer displayOrder;
    private Boolean visible;
    private Instant createdAt;
    private Instant updatedAt;

    public ResumeSectionResponse() {
    }

    public ResumeSectionResponse(ResumeSection section) {
        this.id = section.getId();
        this.resumeProfileId = section.getResumeProfileId();
        this.sectionType = section.getSectionType();
        this.displayOrder = section.getDisplayOrder();
        this.visible = section.getVisible();
        this.createdAt = section.getCreatedAt();
        this.updatedAt = section.getUpdatedAt();
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

    public ResumeSectionType getSectionType() {
        return sectionType;
    }

    public void setSectionType(ResumeSectionType sectionType) {
        this.sectionType = sectionType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
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
