package com.devsphere.user.event;

import com.devsphere.user.entity.ProcessedEvent;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.ProcessedEventRepository;
import com.devsphere.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventConsumerTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private UserRegisteredEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UserRegisteredEventConsumer(userProfileRepository, processedEventRepository);
    }

    @Test
    void consumeUserRegisteredEvent_validEvent_createsUserProfileAndProcessedEvent() {
        Long userId = 301L;
        String eventId = UUID.randomUUID().toString();
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        consumer.consumeUserRegisteredEvent(event);

        verify(userProfileRepository).save(any(UserProfile.class));
        verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
    }

    @Test
    void consumeUserRegisteredEvent_duplicateEventId_skipsProcessing() {
        Long userId = 301L;
        String eventId = "evt-123-duplicate";
        UserRegisteredEvent duplicateEvent = new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        consumer.consumeUserRegisteredEvent(duplicateEvent);

        verify(userProfileRepository, never()).findByUserId(any());
        verify(userProfileRepository, never()).save(any());
        verify(processedEventRepository, never()).saveAndFlush(any());
    }

    @Test
    void consumeUserRegisteredEvent_existingProfileNewEventId_savesProcessedEventWithoutDuplicateProfile() {
        Long userId = 302L;
        String eventId = "evt-456";
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(new UserProfile(userId)));

        consumer.consumeUserRegisteredEvent(event);

        verify(userProfileRepository, never()).save(any());
        verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
    }

    @Test
    void consumeUserRegisteredEvent_concurrentUniqueConstraintViolation_handledSafely() {
        Long userId = 303L;
        String eventId = "evt-race-789";
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key uk_processed_events_event_id"));

        consumer.consumeUserRegisteredEvent(event);

        verify(userProfileRepository, never()).save(any());
        verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
    }

    @Test
    void consumeUserRegisteredEvent_missingUserId_throwsException() {
        UserRegisteredEvent invalidEvent = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_REGISTERED",
                1,
                Instant.now(),
                null
        );

        assertThatThrownBy(() -> consumer.consumeUserRegisteredEvent(invalidEvent))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void consumeUserRegisteredEvent_wrongEventType_throwsException() {
        UserRegisteredEvent invalidEvent = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_DELETED",
                1,
                Instant.now(),
                302L
        );

        assertThatThrownBy(() -> consumer.consumeUserRegisteredEvent(invalidEvent))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void consumeUserRegisteredEvent_unsupportedEventVersion_throwsException() {
        UserRegisteredEvent futureEvent = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_REGISTERED",
                99,
                Instant.now(),
                303L
        );

        assertThatThrownBy(() -> consumer.consumeUserRegisteredEvent(futureEvent))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void consumeUserRegisteredEvent_nullEvent_throwsException() {
        assertThatThrownBy(() -> consumer.consumeUserRegisteredEvent(null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userProfileRepository, never()).save(any());
    }
}
