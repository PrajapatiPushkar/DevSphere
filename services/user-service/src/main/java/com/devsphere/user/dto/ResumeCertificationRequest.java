package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ResumeCertificationRequest {

    @NotNull(message = "certificationId is required")
    private Long certificationId;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public ResumeCertificationRequest() {
    }

    public ResumeCertificationRequest(Long certificationId, Integer displayOrder) {
        this.certificationId = certificationId;
        this.displayOrder = displayOrder;
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
}
