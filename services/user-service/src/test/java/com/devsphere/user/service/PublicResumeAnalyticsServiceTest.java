package com.devsphere.user.service;

import com.devsphere.user.cache.PublicResumeAnalyticsCache;
import com.devsphere.user.dto.publicresume.PublicResumeAnalyticsResponse;
import com.devsphere.user.entity.PublicResumeViewLog;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeStatus;
import com.devsphere.user.entity.ResumeTemplate;
import com.devsphere.user.event.PublicResumeViewEvent;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.PublicResumeViewLogRepository;
import com.devsphere.user.repository.ResumeProfileRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicResumeAnalyticsServiceTest {

    private PublicResumeViewLogRepository viewLogRepository;
    private ResumeProfileRepository resumeProfileRepository;
    private PublicResumeAnalyticsCache analyticsCache;
    private PublicResumeAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        viewLogRepository = mock(PublicResumeViewLogRepository.class);
        resumeProfileRepository = mock(ResumeProfileRepository.class);
        analyticsCache = mock(PublicResumeAnalyticsCache.class);
        analyticsService = new PublicResumeAnalyticsService(viewLogRepository, resumeProfileRepository, analyticsCache);
    }

    @Test
    void onPublicResumeView_hashesIpAndSavesLog() {
        PublicResumeViewEvent event = new PublicResumeViewEvent(
                "pub-uuid-123", 10L, "192.168.1.100", "https://www.linkedin.com/in/test", "Mozilla/5.0"
        );

        analyticsService.onPublicResumeView(event);

        ArgumentCaptor<PublicResumeViewLog> logCaptor = ArgumentCaptor.forClass(PublicResumeViewLog.class);
        verify(viewLogRepository).save(logCaptor.capture());

        PublicResumeViewLog savedLog = logCaptor.getValue();
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getPublicId()).isEqualTo("pub-uuid-123");
        assertThat(savedLog.getResumeProfileId()).isEqualTo(10L);
        assertThat(savedLog.getReferrer()).isEqualTo("www.linkedin.com");
        assertThat(savedLog.getIpHash()).isNotBlank();
        assertThat(savedLog.getIpHash()).doesNotContain("192.168.1.100");
        verify(analyticsCache).evict(10L);
    }

    @Test
    void getResumeAnalytics_whenOwner_returnsAnalyticsResponse() {
        Long userId = 100L;
        Long resumeId = 10L;

        ResumeProfile profile = new ResumeProfile(userId, "Backend Engineer", "Software Engineer", ResumeTemplate.PROFESSIONAL);
        profile.setId(resumeId);
        profile.setStatus(ResumeStatus.DRAFT);

        List<Object[]> referrerRows = new java.util.ArrayList<>();
        referrerRows.add(new Object[]{"linkedin.com", 15L});

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.of(profile));
        when(analyticsCache.get(resumeId)).thenReturn(Optional.empty());
        when(viewLogRepository.countByResumeProfileId(resumeId)).thenReturn(25L);
        when(viewLogRepository.countDistinctIpHashByResumeProfileId(resumeId)).thenReturn(18L);
        when(viewLogRepository.findLatestAccessedAtByResumeProfileId(resumeId)).thenReturn(Optional.of(Instant.now()));
        when(viewLogRepository.findAccessTimestampsSince(eq(resumeId), any())).thenReturn(Collections.emptyList());
        when(viewLogRepository.findTopReferrersByResumeProfileId(eq(resumeId), any())).thenReturn(referrerRows);

        PublicResumeAnalyticsResponse response = analyticsService.getResumeAnalytics(resumeId, userId);

        assertThat(response).isNotNull();
        assertThat(response.getResumeId()).isEqualTo(resumeId);
        assertThat(response.getTotalViews()).isEqualTo(25L);
        assertThat(response.getUniqueVisitors()).isEqualTo(18L);
        assertThat(response.getTopReferrers()).containsEntry("linkedin.com", 15L);
        verify(analyticsCache).put(eq(resumeId), any());
    }

    @Test
    void getResumeAnalytics_whenNotOwner_throwsResourceNotFoundException() {
        Long userId = 100L;
        Long resumeId = 999L;

        when(resumeProfileRepository.findByIdAndUserId(resumeId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getResumeAnalytics(resumeId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resume profile not found");

        verifyNoInteractions(viewLogRepository);
    }

    @Test
    void hashIpAddress_producesConsistentSaltedHash() {
        String hash1 = PublicResumeAnalyticsService.hashIpAddress("10.0.0.1");
        String hash2 = PublicResumeAnalyticsService.hashIpAddress("10.0.0.1");
        String hash3 = PublicResumeAnalyticsService.hashIpAddress("10.0.0.2");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hash3);
        assertThat(hash1).hasSize(64);
    }

    @Test
    void sanitizeReferrer_extractsDomain() {
        assertThat(PublicResumeAnalyticsService.sanitizeReferrer("https://github.com/PrajapatiPushkar")).isEqualTo("github.com");
        assertThat(PublicResumeAnalyticsService.sanitizeReferrer(null)).isEqualTo("direct");
        assertThat(PublicResumeAnalyticsService.sanitizeReferrer("invalid-uri")).isEqualTo("direct");
    }
}
