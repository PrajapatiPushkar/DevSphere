package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ResumeSkillRequest {

    @NotNull(message = "skillId is required")
    private Long skillId;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public ResumeSkillRequest() {
    }

    public ResumeSkillRequest(Long skillId, Integer displayOrder) {
        this.skillId = skillId;
        this.displayOrder = displayOrder;
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
}
