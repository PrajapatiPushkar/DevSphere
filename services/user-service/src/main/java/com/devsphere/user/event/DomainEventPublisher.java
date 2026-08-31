package com.devsphere.user.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
