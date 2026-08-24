package com.devsphere.user.tracing;

import com.devsphere.user.cache.UserProfileCache;
import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.UserProfileRepository;
import com.devsphere.user.service.UserProfileService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTracingTest {

    private UserProfileRepository userProfileRepository;
    private UserProfileCache userProfileCache;
    private SimpleMeterRegistry meterRegistry;
    private Tracer tracer;
    private ScopedSpan scopedSpan;
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileRepository = Mockito.mock(UserProfileRepository.class);
        userProfileCache = Mockito.mock(UserProfileCache.class);
        meterRegistry = new SimpleMeterRegistry();
        tracer = Mockito.mock(Tracer.class);
        scopedSpan = Mockito.mock(ScopedSpan.class);

        when(tracer.startScopedSpan(anyString())).thenReturn(scopedSpan);

        userProfileService = new UserProfileService(
                userProfileRepository,
                userProfileCache,
                meterRegistry,
                tracer
        );
    }

    @Test
    @DisplayName("Should create user.profile.get span on get profile")
    void testGetOrCreateProfileCreatesSpan() {
        UserProfile profile = new UserProfile(200L);
        profile.setFirstName("Alice");

        when(userProfileCache.get(200L)).thenReturn(Optional.empty());
        when(userProfileRepository.findByUserId(200L)).thenReturn(Optional.of(profile));

        UserProfileResponse response = userProfileService.getOrCreateProfile(200L);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(200L);

        verify(tracer).startScopedSpan("user.profile.get");
        verify(scopedSpan).tag("service.operation", "getOrCreateProfile");
        verify(scopedSpan).end();
    }

    @Test
    @DisplayName("Should create user.profile.update span on update profile and protect sensitive data")
    void testUpdateProfileCreatesSpan() {
        UserProfile profile = new UserProfile(200L);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFirstName("Alice");
        request.setLastName("Dev");
        request.setPhoneNumber("+1-555-0199");
        request.setBio("Senior Software Engineer");

        when(userProfileRepository.findByUserId(200L)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        UserProfileResponse response = userProfileService.updateProfile(200L, request);

        assertThat(response).isNotNull();

        verify(tracer).startScopedSpan("user.profile.update");
        verify(scopedSpan).tag("service.operation", "updateProfile");
        verify(scopedSpan).end();

        // Sensitive data protection: phone number, bio, name must never be added as span tags
        verify(scopedSpan, never()).tag(anyString(), Mockito.eq("+1-555-0199"));
        verify(scopedSpan, never()).tag(anyString(), Mockito.eq("Senior Software Engineer"));
    }
}
