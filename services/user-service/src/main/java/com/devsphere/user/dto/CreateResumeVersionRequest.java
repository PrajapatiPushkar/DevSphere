package com.devsphere.user.dto;

import jakarta.validation.constraints.Size;

public class CreateResumeVersionRequest {

    @Size(max = 255, message = "version name cannot exceed 255 characters")
    private String name;

    public CreateResumeVersionRequest() {
    }

    public CreateResumeVersionRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
