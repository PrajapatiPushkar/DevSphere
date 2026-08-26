package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ResumeExperienceRequest {

    @NotNull(message = "experienceId is required")
    private Long experienceId;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public ResumeExperienceRequest() {
    }

    public ResumeExperienceRequest(Long experienceId, Integer displayOrder) {
        this.experienceId = experienceId;
        this.displayOrder = displayOrder;
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
}
