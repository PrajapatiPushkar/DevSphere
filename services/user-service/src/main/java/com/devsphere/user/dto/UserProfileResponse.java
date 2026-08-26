package com.devsphere.user.dto;

import java.time.Instant;

public class UserProfileResponse {

    private Long userId;
    private String firstName;
    private String lastName;
    private String displayName;
    private String headline;
    private String bio;
    private String location;
    private String phoneNumber;
    private String githubUrl;
    private String linkedinUrl;
    private String portfolioUrl;
    private String currentRole;
    private Integer yearsOfExperience;
    private Instant createdAt;
    private Instant updatedAt;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long userId, String firstName, String lastName, String displayName, String bio, String phoneNumber, Instant createdAt, Instant updatedAt) {
        this(userId, firstName, lastName, displayName, null, bio, null, phoneNumber, null, null, null, null, null, createdAt, updatedAt);
    }

    public UserProfileResponse(Long userId, String firstName, String lastName, String displayName, String headline, String bio, String location, String phoneNumber, String githubUrl, String linkedinUrl, String portfolioUrl, String currentRole, Integer yearsOfExperience, Instant createdAt, Instant updatedAt) {
        this.userId = userId;
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
