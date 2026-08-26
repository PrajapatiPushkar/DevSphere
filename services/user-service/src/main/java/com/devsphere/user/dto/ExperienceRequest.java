package com.devsphere.user.dto;

import com.devsphere.user.entity.EmploymentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ExperienceRequest {

    @NotBlank(message = "companyName is required")
    @Size(max = 255, message = "companyName cannot exceed 255 characters")
    private String companyName;

    @NotBlank(message = "jobTitle is required")
    @Size(max = 255, message = "jobTitle cannot exceed 255 characters")
    private String jobTitle;

    @NotNull(message = "employmentType is required")
    private EmploymentType employmentType;

    @Size(max = 255, message = "location cannot exceed 255 characters")
    private String location;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean currentlyWorking;

    @Size(max = 4000, message = "description cannot exceed 4000 characters")
    private String description;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public ExperienceRequest() {
    }

    public ExperienceRequest(String companyName, String jobTitle, EmploymentType employmentType, String location, LocalDate startDate, LocalDate endDate, Boolean currentlyWorking, String description, Integer displayOrder) {
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.employmentType = employmentType;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyWorking = currentlyWorking;
        this.description = description;
        this.displayOrder = displayOrder;
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
}
