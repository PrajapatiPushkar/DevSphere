package com.devsphere.user.dto;

import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeStatus;
import com.devsphere.user.entity.ResumeTemplate;
import java.time.Instant;

public class ResumeProfileResponse {

    private Long id;
    private Long userId;
    private String name;
    private String targetRole;
    private String summaryOverride;
    private ResumeTemplate template;
    private ResumeStatus status;
    private String publicId;
    private Instant createdAt;
    private Instant updatedAt;

    public ResumeProfileResponse() {
    }

    public ResumeProfileResponse(ResumeProfile profile) {
        this.id = profile.getId();
        this.userId = profile.getUserId();
        this.name = profile.getName();
        this.targetRole = profile.getTargetRole();
        this.summaryOverride = profile.getSummaryOverride();
        this.template = profile.getTemplate();
        this.status = profile.getStatus();
        this.publicId = profile.getPublicId();
        this.createdAt = profile.getCreatedAt();
        this.updatedAt = profile.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getSummaryOverride() {
        return summaryOverride;
    }

    public void setSummaryOverride(String summaryOverride) {
        this.summaryOverride = summaryOverride;
    }

    public ResumeTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ResumeTemplate template) {
        this.template = template;
    }

    public ResumeStatus getStatus() {
        return status;
    }

    public void setStatus(ResumeStatus status) {
        this.status = status;
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

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }
}
