package com.devsphere.auth.event;

public class UserRegisteredDomainEvent {

    private final Long userId;

    public UserRegisteredDomainEvent(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
