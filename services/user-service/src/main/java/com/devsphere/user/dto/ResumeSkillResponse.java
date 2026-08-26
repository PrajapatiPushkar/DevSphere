package com.devsphere.user.dto;

import com.devsphere.user.entity.ResumeSkill;
import java.time.Instant;

public class ResumeSkillResponse {

    private Long id;
    private Long resumeProfileId;
    private Long skillId;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public ResumeSkillResponse() {
    }

    public ResumeSkillResponse(ResumeSkill rs) {
        this.id = rs.getId();
        this.resumeProfileId = rs.getResumeProfileId();
        this.skillId = rs.getSkillId();
        this.displayOrder = rs.getDisplayOrder();
        this.createdAt = rs.getCreatedAt();
        this.updatedAt = rs.getUpdatedAt();
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

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
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
