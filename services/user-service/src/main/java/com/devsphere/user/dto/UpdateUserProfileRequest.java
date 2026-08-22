package com.devsphere.user.dto;

import jakarta.validation.constraints.Size;

public class UpdateUserProfileRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phoneNumber;

    public UpdateUserProfileRequest() {
    }

    public UpdateUserProfileRequest(String firstName, String lastName, String displayName, String bio, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.bio = bio;
        this.phoneNumber = phoneNumber;
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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
