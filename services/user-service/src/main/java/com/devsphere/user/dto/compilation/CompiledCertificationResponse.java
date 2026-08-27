package com.devsphere.user.dto.compilation;

import com.devsphere.user.entity.Certification;
import java.time.LocalDate;

public class CompiledCertificationResponse {

    private Long id;
    private String name;
    private String issuingOrganization;
    private LocalDate issueDate;
    private LocalDate expirationDate;
    private String credentialId;
    private String credentialUrl;
    private String description;
    private Integer displayOrder;

    public CompiledCertificationResponse() {
    }

    public CompiledCertificationResponse(Certification cert, Integer displayOrder) {
        this.id = cert.getId();
        this.name = cert.getName();
        this.issuingOrganization = cert.getIssuingOrganization();
        this.issueDate = cert.getIssueDate();
        this.expirationDate = cert.getExpirationDate();
        this.credentialId = cert.getCredentialId();
        this.credentialUrl = cert.getCredentialUrl();
        this.description = cert.getDescription();
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
