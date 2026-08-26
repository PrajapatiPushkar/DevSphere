package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ResumeProjectRequest {

    @NotNull(message = "projectId is required")
    private Long projectId;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public ResumeProjectRequest() {
    }

    public ResumeProjectRequest(Long projectId, Integer displayOrder) {
        this.projectId = projectId;
        this.displayOrder = displayOrder;
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
}
