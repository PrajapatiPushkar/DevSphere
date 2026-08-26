package com.devsphere.user.dto;

import com.devsphere.user.entity.Availability;
import com.devsphere.user.entity.CareerProfile;
import com.devsphere.user.entity.WorkPreference;
import java.time.Instant;

public class CareerProfileResponse {

    private Long id;
    private Long userId;
    private String professionalSummary;
    private String currentTitle;
    private String targetRole;
    private Integer yearsOfExperience;
    private String preferredLocation;
    private WorkPreference workPreference;
    private Availability availability;
    private Instant createdAt;
    private Instant updatedAt;

    public CareerProfileResponse() {
    }

    public CareerProfileResponse(CareerProfile profile) {
        this.id = profile.getId();
        this.userId = profile.getUserId();
        this.professionalSummary = profile.getProfessionalSummary();
        this.currentTitle = profile.getCurrentTitle();
        this.targetRole = profile.getTargetRole();
        this.yearsOfExperience = profile.getYearsOfExperience();
        this.preferredLocation = profile.getPreferredLocation();
        this.workPreference = profile.getWorkPreference();
        this.availability = profile.getAvailability();
        this.createdAt = profile.getCreatedAt();
        this.updatedAt = profile.getUpdatedAt();
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
