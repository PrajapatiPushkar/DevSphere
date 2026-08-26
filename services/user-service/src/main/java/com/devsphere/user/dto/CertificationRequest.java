package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class CertificationRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "issuingOrganization is required")
    @Size(max = 255, message = "issuingOrganization cannot exceed 255 characters")
    private String issuingOrganization;

    private LocalDate issueDate;

    private LocalDate expirationDate;

    @Size(max = 255, message = "credentialId cannot exceed 255 characters")
    private String credentialId;

    @Pattern(regexp = "^(https?://.+)?$", message = "credentialUrl must be a valid HTTP or HTTPS URL")
    @Size(max = 1000, message = "credentialUrl cannot exceed 1000 characters")
    private String credentialUrl;

    @Size(max = 2000, message = "description cannot exceed 2000 characters")
    private String description;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public CertificationRequest() {
    }

    public CertificationRequest(String name, String issuingOrganization, LocalDate issueDate, LocalDate expirationDate, String credentialId, String credentialUrl, String description, Integer displayOrder) {
        this.name = name;
        this.issuingOrganization = issuingOrganization;
        this.issueDate = issueDate;
        this.expirationDate = expirationDate;
        this.credentialId = credentialId;
        this.credentialUrl = credentialUrl;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuingOrganization() {
        return issuingOrganization;
    }

    public void setIssuingOrganization(String issuingOrganization) {
        this.issuingOrganization = issuingOrganization;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getCredentialUrl() {
        return credentialUrl;
    }

    public void setCredentialUrl(String credentialUrl) {
        this.credentialUrl = credentialUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
