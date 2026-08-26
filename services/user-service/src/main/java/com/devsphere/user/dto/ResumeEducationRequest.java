package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ResumeEducationRequest {

    @NotNull(message = "educationId is required")
    private Long educationId;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public ResumeEducationRequest() {
    }

    public ResumeEducationRequest(Long educationId, Integer displayOrder) {
        this.educationId = educationId;
        this.displayOrder = displayOrder;
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
}
