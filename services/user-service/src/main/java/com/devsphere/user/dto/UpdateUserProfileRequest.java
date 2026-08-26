package com.devsphere.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateUserProfileRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    @Size(max = 250, message = "Headline must not exceed 250 characters")
    private String headline;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phoneNumber;

    @Pattern(regexp = "^(https?://.+)?$", message = "GitHub URL must be a valid HTTP or HTTPS URL")
    @Size(max = 255, message = "GitHub URL must not exceed 255 characters")
    private String githubUrl;

    @Pattern(regexp = "^(https?://.+)?$", message = "LinkedIn URL must be a valid HTTP or HTTPS URL")
    @Size(max = 255, message = "LinkedIn URL must not exceed 255 characters")
    private String linkedinUrl;

    @Pattern(regexp = "^(https?://.+)?$", message = "Portfolio URL must be a valid HTTP or HTTPS URL")
    @Size(max = 255, message = "Portfolio URL must not exceed 255 characters")
    private String portfolioUrl;

    @Size(max = 100, message = "Current role must not exceed 100 characters")
    private String currentRole;

    @Min(value = 0, message = "Years of experience must not be negative")
    private Integer yearsOfExperience;

    public UpdateUserProfileRequest() {
    }

    public UpdateUserProfileRequest(String firstName, String lastName, String displayName, String bio, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.bio = bio;
        this.phoneNumber = phoneNumber;
    }

    public UpdateUserProfileRequest(String firstName, String lastName, String displayName, String headline, String bio, String location, String phoneNumber, String githubUrl, String linkedinUrl, String portfolioUrl, String currentRole, Integer yearsOfExperience) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.headline = headline;
        this.bio = bio;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.portfolioUrl = portfolioUrl;
        this.currentRole = currentRole;
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }
}
