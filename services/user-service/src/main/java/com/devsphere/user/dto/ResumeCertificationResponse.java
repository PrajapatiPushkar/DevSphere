package com.devsphere.user.dto;

import com.devsphere.user.entity.ResumeCertification;
import java.time.Instant;

public class ResumeCertificationResponse {

    private Long id;
    private Long resumeProfileId;
    private Long certificationId;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public ResumeCertificationResponse() {
    }

    public ResumeCertificationResponse(ResumeCertification rc) {
        this.id = rc.getId();
        this.resumeProfileId = rc.getResumeProfileId();
        this.certificationId = rc.getCertificationId();
        this.displayOrder = rc.getDisplayOrder();
        this.createdAt = rc.getCreatedAt();
        this.updatedAt = rc.getUpdatedAt();
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

    public Long getCertificationId() {
        return certificationId;
    }

    public void setCertificationId(Long certificationId) {
        this.certificationId = certificationId;
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
