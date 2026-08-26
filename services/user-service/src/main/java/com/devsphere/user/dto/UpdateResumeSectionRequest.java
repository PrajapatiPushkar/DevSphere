package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateResumeSectionRequest {

    @Min(value = 1, message = "displayOrder must be at least 1")
    private Integer displayOrder;

    @NotNull(message = "visible is required")
    private Boolean visible;

    public UpdateResumeSectionRequest() {
    }

    public UpdateResumeSectionRequest(Integer displayOrder, Boolean visible) {
        this.displayOrder = displayOrder;
        this.visible = visible;
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
}
