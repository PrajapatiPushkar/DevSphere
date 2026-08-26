package com.devsphere.user.dto;

import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import java.time.Instant;
import java.time.LocalDate;

public class ExperienceResponse {

    private Long id;
    private Long userId;
    private String companyName;
    private String jobTitle;
    private EmploymentType employmentType;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentlyWorking;
    private String description;
    private Integer displayOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public ExperienceResponse() {
    }

    public ExperienceResponse(Experience experience) {
        this.id = experience.getId();
        this.userId = experience.getUserId();
        this.companyName = experience.getCompanyName();
        this.jobTitle = experience.getJobTitle();
        this.employmentType = experience.getEmploymentType();
        this.location = experience.getLocation();
        this.startDate = experience.getStartDate();
        this.endDate = experience.getEndDate();
        this.currentlyWorking = experience.getCurrentlyWorking();
        this.description = experience.getDescription();
        this.displayOrder = experience.getDisplayOrder();
        this.createdAt = experience.getCreatedAt();
        this.updatedAt = experience.getUpdatedAt();
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getCurrentlyWorking() {
        return currentlyWorking;
    }

    public void setCurrentlyWorking(Boolean currentlyWorking) {
        this.currentlyWorking = currentlyWorking;
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
