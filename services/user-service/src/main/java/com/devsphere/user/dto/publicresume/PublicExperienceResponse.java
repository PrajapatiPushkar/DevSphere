package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledExperienceResponse;
import com.devsphere.user.entity.EmploymentType;
import java.time.LocalDate;

public class PublicExperienceResponse {

    private String companyName;
    private String jobTitle;
    private EmploymentType employmentType;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentlyWorking;
    private String description;
    private Integer displayOrder;

    public PublicExperienceResponse() {
    }

    public PublicExperienceResponse(String companyName, String jobTitle, EmploymentType employmentType,
                                    String location, LocalDate startDate, LocalDate endDate,
                                    Boolean currentlyWorking, String description, Integer displayOrder) {
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

    public PublicExperienceResponse(CompiledExperienceResponse compiled) {
        if (compiled != null) {
            this.companyName = compiled.getCompanyName();
            this.jobTitle = compiled.getJobTitle();
            this.employmentType = compiled.getEmploymentType();
            this.location = compiled.getLocation();
            this.startDate = compiled.getStartDate();
            this.endDate = compiled.getEndDate();
            this.currentlyWorking = compiled.getCurrentlyWorking();
            this.description = compiled.getDescription();
            this.displayOrder = compiled.getDisplayOrder();
        }
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
