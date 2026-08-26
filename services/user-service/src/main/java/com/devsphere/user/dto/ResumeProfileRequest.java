package com.devsphere.user.dto;

import com.devsphere.user.entity.ResumeTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResumeProfileRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "targetRole is required")
    @Size(max = 255, message = "targetRole cannot exceed 255 characters")
    private String targetRole;

    @Size(max = 4000, message = "summaryOverride cannot exceed 4000 characters")
    private String summaryOverride;

    @NotNull(message = "template is required")
    private ResumeTemplate template;

    public ResumeProfileRequest() {
    }

    public ResumeProfileRequest(String name, String targetRole, String summaryOverride, ResumeTemplate template) {
        this.name = name;
        this.targetRole = targetRole;
        this.summaryOverride = summaryOverride;
        this.template = template;
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
}
