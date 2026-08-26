package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class EducationRequest {

    @NotBlank(message = "institutionName is required")
    @Size(max = 255, message = "institutionName cannot exceed 255 characters")
    private String institutionName;

    @NotBlank(message = "degree is required")
    @Size(max = 255, message = "degree cannot exceed 255 characters")
    private String degree;

    @Size(max = 255, message = "fieldOfStudy cannot exceed 255 characters")
    private String fieldOfStudy;

    @Size(max = 255, message = "location cannot exceed 255 characters")
    private String location;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean currentlyStudying;

    @Size(max = 4000, message = "description cannot exceed 4000 characters")
    private String description;

    @Min(value = 0, message = "displayOrder must be zero or positive")
    private Integer displayOrder;

    public EducationRequest() {
    }

    public EducationRequest(String institutionName, String degree, String fieldOfStudy, String location, LocalDate startDate, LocalDate endDate, Boolean currentlyStudying, String description, Integer displayOrder) {
        this.institutionName = institutionName;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyStudying = currentlyStudying;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
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

    public Boolean getCurrentlyStudying() {
        return currentlyStudying;
    }

    public void setCurrentlyStudying(Boolean currentlyStudying) {
        this.currentlyStudying = currentlyStudying;
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
