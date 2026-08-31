package com.devsphere.user.cache;

import com.devsphere.user.dto.ResumeVersionResponse;
import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.entity.ResumeVersion;
import com.devsphere.user.entity.ResumeVersionStatus;
import com.devsphere.user.entity.UserProfile;
import com.devsphere.user.repository.ResumeProfileRepository;
import com.devsphere.user.repository.ResumeVersionRepository;
import com.devsphere.user.repository.UserProfileRepository;
import com.devsphere.user.service.PublicResumeService;
import com.devsphere.user.service.ResumeCompilationService;
import com.devsphere.user.service.ResumeProfileService;
import com.devsphere.user.service.ResumeVersionService;
import com.devsphere.user.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CacheConsistencyAndInvalidationTest {

    private RedisTemplate<String, UserProfileResponse> profileRedisTemplate;
    private RedisTemplate<String, Object> publicResumeRedisTemplate;
    private ValueOperations<String, UserProfileResponse> profileValueOps;
    private ValueOperations<String, Object> publicResumeValueOps;

    private UserProfileRepository userProfileRepository;
    private ResumeProfileRepository resumeProfileRepository;
    private ResumeVersionRepository resumeVersionRepository;
    private ResumeCompilationService resumeCompilationService;

    private MeterRegistry meterRegistry;
    private RedisUserProfileCache userProfileCache;
    private RedisPublicResumeCache publicResumeCache;

    private UserProfileService userProfileService;
    private ResumeVersionService resumeVersionService;
    private ResumeProfileService resumeProfileService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        profileRedisTemplate = mock(RedisTemplate.class);
        publicResumeRedisTemplate = mock(RedisTemplate.class);
        profileValueOps = mock(ValueOperations.class);
        publicResumeValueOps = mock(ValueOperations.class);

        when(profileRedisTemplate.opsForValue()).thenReturn(profileValueOps);
        when(publicResumeRedisTemplate.opsForValue()).thenReturn(publicResumeValueOps);

        userProfileRepository = mock(UserProfileRepository.class);
        resumeProfileRepository = mock(ResumeProfileRepository.class);
        resumeVersionRepository = mock(ResumeVersionRepository.class);
        resumeCompilationService = mock(ResumeCompilationService.class);

        meterRegistry = new SimpleMeterRegistry();

        userProfileCache = new RedisUserProfileCache(profileRedisTemplate, meterRegistry, Duration.ofMinutes(5));
        publicResumeCache = new RedisPublicResumeCache(publicResumeRedisTemplate, meterRegistry, Duration.ofMinutes(10));

        userProfileService = new UserProfileService(userProfileRepository, userProfileCache, meterRegistry, null);
        resumeVersionService = new ResumeVersionService(resumeVersionRepository, resumeProfileRepository, resumeCompilationService, new ObjectMapper(), publicResumeCache, null, meterRegistry);
        resumeProfileService = new ResumeProfileService(resumeProfileRepository, mock(com.devsphere.user.repository.ResumeSectionRepository.class), publicResumeCache, meterRegistry);
    }

    @Test
    @DisplayName("Scenario A: Profile update invalidates affected user profile cache entry")
    void updateProfileInvalidatesUserCache() {
        Long userId = 100L;
        UserProfile existing = new UserProfile(userId);
        existing.setFirstName("OldFirstName");

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFirstName("NewFirstName");

        UserProfileResponse updated = userProfileService.updateProfile(userId, request);

        assertNotNull(updated);
        assertEquals("NewFirstName", updated.getFirstName());
        verify(profileRedisTemplate, times(1)).delete("user-profile:100");
        assertTrue(meterRegistry.counter("devsphere.cache.invalidation.success.total", "cache", "user_profile").count() > 0);
    }

    @Test
    @DisplayName("Scenario A: Publishing a version invalidates affected public resume cache")
    void publishVersionInvalidatesPublicResumeCache() {
        Long resumeId = 10L;
        Long versionId = 5L;
        Long userId = 200L;
        String publicId = "pub-uuid-1234";

        ResumeProfile profile = new ResumeProfile(userId, "Backend Engineer", "Senior Developer", ResumeTemplate.MODERN);
        profile.setId(resumeId);
        profile.setPublicId(publicId);

        ResumeVersion version = new ResumeVersion(resumeId, userId, 1, "V1 Draft", "{\"id\":10,\"name\":\"Backend Engineer\",\"sections\":[]}");
        version.setId(versionId);
        version.setStatus(ResumeVersionStatus.DRAFT);

        when(resumeProfileRepository.findByIdAndUserIdForUpdate(resumeId, userId)).thenReturn(Optional.of(profile));
        when(resumeVersionRepository.findByIdAndResumeProfileIdAndUserId(versionId, resumeId, userId)).thenReturn(Optional.of(version));
        when(resumeVersionRepository.findByResumeProfileIdAndStatus(resumeId, ResumeVersionStatus.PUBLISHED)).thenReturn(Optional.empty());
        when(resumeVersionRepository.save(any(ResumeVersion.class))).thenAnswer(i -> i.getArgument(0));

        ResumeVersionResponse response = resumeVersionService.publishVersion(resumeId, versionId, userId);

        assertNotNull(response);
        assertEquals(ResumeVersionStatus.PUBLISHED, response.getStatus());
        verify(publicResumeRedisTemplate, times(1)).delete("public-resume:pub-uuid-1234");
        assertTrue(meterRegistry.counter("devsphere.cache.invalidation.success.total", "cache", "public_resume").count() > 0);
    }

    @Test
    @DisplayName("Scenario B: Redis failure during invalidation does NOT fail primary database operation")
    void redisFailureDuringEvictionDoesNotFailDbOperation() {
        Long userId = 300L;
        UserProfile existing = new UserProfile(userId);

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profileRedisTemplate.delete("user-profile:300")).thenThrow(new RedisConnectionFailureException("Redis connection lost"));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setLastName("UpdatedLastName");

        // DB update must succeed cleanly despite Redis error
        UserProfileResponse response = assertDoesNotThrow(() -> userProfileService.updateProfile(userId, request));

        assertNotNull(response);
        assertEquals("UpdatedLastName", response.getLastName());
        assertTrue(meterRegistry.counter("devsphere.cache.invalidation.failures.total", "cache", "user_profile").count() > 0);
        assertTrue(meterRegistry.counter("devsphere_resilience_fallback_total", "service", "user-service", "dependency", "redis").count() > 0);
    }

    @Test
    @DisplayName("Scenario D: Targeted invalidation deletes only affected key without touching unrelated cache keys")
    void targetedInvalidationDeletesOnlyAffectedKey() {
        Long userId = 400L;
        UserProfile existing = new UserProfile(userId);

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setHeadline("Tech Lead");

        userProfileService.updateProfile(userId, request);

        verify(profileRedisTemplate, times(1)).delete("user-profile:400");
        verify(profileRedisTemplate, never()).delete("user-profile:500");
        verify(publicResumeRedisTemplate, never()).delete(anyString());
    }
}
