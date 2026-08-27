package com.devsphere.user.dto.compilation;

import com.devsphere.user.entity.EmploymentType;
import com.devsphere.user.entity.Experience;
import java.time.LocalDate;

public class CompiledExperienceResponse {

    private Long id;
    private String companyName;
    private String jobTitle;
    private EmploymentType employmentType;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentlyWorking;
    private String description;
    private Integer displayOrder;

    public CompiledExperienceResponse() {
    }

    public CompiledExperienceResponse(Experience exp, Integer displayOrder) {
        this.id = exp.getId();
        this.companyName = exp.getCompanyName();
        this.jobTitle = exp.getJobTitle();
        this.employmentType = exp.getEmploymentType();
        this.location = exp.getLocation();
        this.startDate = exp.getStartDate();
        this.endDate = exp.getEndDate();
        this.currentlyWorking = exp.getCurrentlyWorking();
        this.description = exp.getDescription();
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
