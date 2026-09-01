package com.devsphere.user.repository;

import com.devsphere.user.entity.ProcessedEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    Optional<ProcessedEvent> findByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    boolean existsByEventId(String eventId);

    Optional<ProcessedEvent> findByEventId(String eventId);
}
