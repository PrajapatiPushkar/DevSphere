package com.devsphere.user.dto;

import com.devsphere.user.entity.Availability;
import com.devsphere.user.entity.WorkPreference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class CareerProfileRequest {

    @Size(max = 2000, message = "professionalSummary cannot exceed 2000 characters")
    private String professionalSummary;

    @Size(max = 255, message = "currentTitle cannot exceed 255 characters")
    private String currentTitle;

    @Size(max = 255, message = "targetRole cannot exceed 255 characters")
    private String targetRole;

    @Min(value = 0, message = "yearsOfExperience must not be negative")
    @Max(value = 70, message = "yearsOfExperience must be realistic (max 70)")
    private Integer yearsOfExperience;

    @Size(max = 255, message = "preferredLocation cannot exceed 255 characters")
    private String preferredLocation;

    private WorkPreference workPreference;

    private Availability availability;

    public CareerProfileRequest() {
    }

    public CareerProfileRequest(String professionalSummary, String currentTitle, String targetRole, Integer yearsOfExperience, String preferredLocation, WorkPreference workPreference, Availability availability) {
        this.professionalSummary = professionalSummary;
        this.currentTitle = currentTitle;
        this.targetRole = targetRole;
        this.yearsOfExperience = yearsOfExperience;
        this.preferredLocation = preferredLocation;
        this.workPreference = workPreference;
        this.availability = availability;
    }

    public String getProfessionalSummary() {
        return professionalSummary;
    }

    public void setProfessionalSummary(String professionalSummary) {
        this.professionalSummary = professionalSummary;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public WorkPreference getWorkPreference() {
        return workPreference;
    }

    public void setWorkPreference(WorkPreference workPreference) {
        this.workPreference = workPreference;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }
}
