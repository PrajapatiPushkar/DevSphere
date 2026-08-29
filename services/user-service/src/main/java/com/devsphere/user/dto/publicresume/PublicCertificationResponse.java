package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledCertificationResponse;
import java.time.LocalDate;

public class PublicCertificationResponse {

    private String name;
    private String issuingOrganization;
    private LocalDate issueDate;
    private LocalDate expirationDate;
    private String credentialId;
    private String credentialUrl;
    private String description;
    private Integer displayOrder;

    public PublicCertificationResponse() {
    }

    public PublicCertificationResponse(String name, String issuingOrganization, LocalDate issueDate,
                                       LocalDate expirationDate, String credentialId, String credentialUrl,
                                       String description, Integer displayOrder) {
        this.name = name;
        this.issuingOrganization = issuingOrganization;
        this.issueDate = issueDate;
        this.expirationDate = expirationDate;
        this.credentialId = credentialId;
        this.credentialUrl = credentialUrl;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public PublicCertificationResponse(CompiledCertificationResponse compiled) {
        if (compiled != null) {
            this.name = compiled.getName();
            this.issuingOrganization = compiled.getIssuingOrganization();
            this.issueDate = compiled.getIssueDate();
            this.expirationDate = compiled.getExpirationDate();
            this.credentialId = compiled.getCredentialId();
            this.credentialUrl = compiled.getCredentialUrl();
            this.description = compiled.getDescription();
            this.displayOrder = compiled.getDisplayOrder();
        }
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
