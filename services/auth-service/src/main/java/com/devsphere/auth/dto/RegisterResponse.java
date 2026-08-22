package com.devsphere.auth.dto;

import java.time.Instant;

public class RegisterResponse {

    private Long id;
    private String email;
    private Instant createdAt;

    public RegisterResponse() {
    }

    public RegisterResponse(Long id, String email, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
