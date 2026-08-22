package com.devsphere.user.event;

import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventConsumerTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserRegisteredEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new UserRegisteredEventConsumer(userProfileRepository);
    }

    @Test
    void consumeUserRegisteredEvent_validEvent_createsUserProfile() {
        Long userId = 301L;
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        consumer.consumeUserRegisteredEvent(event);

        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void consumeUserRegisteredEvent_duplicateEvent_doesNotCreateDuplicateProfile() {
        Long userId = 301L;
        UserRegisteredEvent duplicateEvent = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_REGISTERED",
                1,
                Instant.now(),
                userId
        );

        UserProfile existingProfile = new UserProfile(userId);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        consumer.consumeUserRegisteredEvent(duplicateEvent);

        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void consumeUserRegisteredEvent_missingUserId_skipsProcessing() {
        UserRegisteredEvent invalidEvent = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_REGISTERED",
                1,
                Instant.now(),
                null
        );

        consumer.consumeUserRegisteredEvent(invalidEvent);

        verify(userProfileRepository, never()).findByUserId(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void consumeUserRegisteredEvent_wrongEventType_skipsProcessing() {
        UserRegisteredEvent invalidEvent = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_DELETED",
                1,
                Instant.now(),
                302L
        );

        consumer.consumeUserRegisteredEvent(invalidEvent);

        verify(userProfileRepository, never()).findByUserId(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void consumeUserRegisteredEvent_unsupportedEventVersion_skipsProcessing() {
        UserRegisteredEvent futureEvent = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "USER_REGISTERED",
                99,
                Instant.now(),
                303L
        );

        consumer.consumeUserRegisteredEvent(futureEvent);

        verify(userProfileRepository, never()).findByUserId(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void consumeUserRegisteredEvent_nullEvent_handlesGracefully() {
        consumer.consumeUserRegisteredEvent(null);

        verify(userProfileRepository, never()).findByUserId(any());
        verify(userProfileRepository, never()).save(any());
    }
}
