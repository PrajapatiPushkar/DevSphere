package com.devsphere.user.dto;

import com.devsphere.user.entity.Certification;
import java.time.Instant;
import java.time.LocalDate;

public class CertificationResponse {

    private Long id;
    private Long userId;
    private String name;
    private String issuingOrganization;
    private LocalDate issueDate;
    private LocalDate expirationDate;
    private String credentialId;
    private String credentialUrl;
    private String description;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public CertificationResponse() {
    }

    public CertificationResponse(Certification certification) {
        this.id = certification.getId();
        this.userId = certification.getUserId();
        this.name = certification.getName();
        this.issuingOrganization = certification.getIssuingOrganization();
        this.issueDate = certification.getIssueDate();
        this.expirationDate = certification.getExpirationDate();
        this.credentialId = certification.getCredentialId();
        this.credentialUrl = certification.getCredentialUrl();
        this.description = certification.getDescription();
        this.displayOrder = certification.getDisplayOrder();
        this.createdAt = certification.getCreatedAt();
        this.updatedAt = certification.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
