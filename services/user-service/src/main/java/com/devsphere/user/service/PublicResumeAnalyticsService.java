package com.devsphere.user.service;

import com.devsphere.user.cache.PublicResumeAnalyticsCache;
import com.devsphere.user.dto.publicresume.PublicResumeAnalyticsResponse;
import com.devsphere.user.entity.PublicResumeViewLog;
import com.devsphere.user.entity.ResumeProfile;
import com.devsphere.user.entity.ResumeStatus;
import com.devsphere.user.event.PublicResumeViewEvent;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.PublicResumeViewLogRepository;
import com.devsphere.user.repository.ResumeProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicResumeAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(PublicResumeAnalyticsService.class);
    private static final String IP_SALT = "devsphere-privacy-salt-2026";

    private final PublicResumeViewLogRepository viewLogRepository;
    private final ResumeProfileRepository resumeProfileRepository;
    private final PublicResumeAnalyticsCache analyticsCache;
    private final MeterRegistry meterRegistry;

    public PublicResumeAnalyticsService(
            PublicResumeViewLogRepository viewLogRepository,
            ResumeProfileRepository resumeProfileRepository) {
        this(viewLogRepository, resumeProfileRepository, null, new SimpleMeterRegistry());
    }

    public PublicResumeAnalyticsService(
            PublicResumeViewLogRepository viewLogRepository,
            ResumeProfileRepository resumeProfileRepository,
            PublicResumeAnalyticsCache analyticsCache) {
        this(viewLogRepository, resumeProfileRepository, analyticsCache, new SimpleMeterRegistry());
    }

    @Autowired(required = false)
    public PublicResumeAnalyticsService(
            PublicResumeViewLogRepository viewLogRepository,
            ResumeProfileRepository resumeProfileRepository,
            PublicResumeAnalyticsCache analyticsCache,
            MeterRegistry meterRegistry) {
        this.viewLogRepository = viewLogRepository;
        this.resumeProfileRepository = resumeProfileRepository;
        this.analyticsCache = analyticsCache;
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    @Async
    @EventListener
    public void onPublicResumeView(PublicResumeViewEvent event) {
        if (event == null || event.getPublicId() == null || event.getResumeProfileId() == null) {
            return;
        }

        try {
            String ipHash = hashIpAddress(event.getClientIp());
            String referrerDomain = sanitizeReferrer(event.getReferrer());
            String truncatedUserAgent = truncate(event.getUserAgent(), 500);

            PublicResumeViewLog viewLog = new PublicResumeViewLog(
                    event.getPublicId(),
                    event.getResumeProfileId(),
                    ipHash,
                    referrerDomain,
                    truncatedUserAgent
            );

            viewLogRepository.save(viewLog);

            if (analyticsCache != null) {
                analyticsCache.evict(event.getResumeProfileId());
            }

            meterRegistry.counter("devsphere_public_resume_views_total", "status", "recorded").increment();
            log.info("Recorded public resume view for publicId={}, profileId={}", event.getPublicId(), event.getResumeProfileId());
        } catch (Exception e) {
            meterRegistry.counter("devsphere_public_resume_views_total", "status", "error").increment();
            log.error("Failed to record public resume view for publicId={}: {}", event.getPublicId(), e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PublicResumeAnalyticsResponse getResumeAnalytics(Long resumeId, Long userId) {
        ResumeProfile profile = resumeProfileRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found"));

        if (profile.getStatus() == ResumeStatus.ARCHIVED) {
            throw new ResourceNotFoundException("RESUME_NOT_FOUND", "Resume profile not found");
        }

        if (analyticsCache != null) {
            Optional<PublicResumeAnalyticsResponse> cached = analyticsCache.get(resumeId);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        long totalViews = viewLogRepository.countByResumeProfileId(resumeId);
        long uniqueVisitors = viewLogRepository.countDistinctIpHashByResumeProfileId(resumeId);
        Instant lastAccessedAt = viewLogRepository.findLatestAccessedAtByResumeProfileId(resumeId).orElse(null);

        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Instant> recentTimestamps = viewLogRepository.findAccessTimestampsSince(resumeId, thirtyDaysAgo);

        Map<String, Long> viewsByDay = aggregateViewsByDay(recentTimestamps);

        List<Object[]> referrerRows = viewLogRepository.findTopReferrersByResumeProfileId(resumeId, PageRequest.of(0, 5));
        Map<String, Long> topReferrers = new LinkedHashMap<>();
        for (Object[] row : referrerRows) {
            String domain = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            topReferrers.put(domain, count);
        }

        PublicResumeAnalyticsResponse response = new PublicResumeAnalyticsResponse(
                resumeId,
                profile.getPublicId(),
                totalViews,
                uniqueVisitors,
                lastAccessedAt,
                viewsByDay,
                topReferrers
        );

        if (analyticsCache != null) {
            analyticsCache.put(resumeId, response);
        }

        return response;
    }

    public static String hashIpAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest((IP_SALT + ":" + ip.trim()).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "hash_error";
        }
    }

    public static String sanitizeReferrer(String referrer) {
        if (referrer == null || referrer.isBlank()) {
            return "direct";
        }
        try {
            URI uri = new URI(referrer.trim());
            String host = uri.getHost();
            if (host != null && !host.isBlank()) {
                return host.toLowerCase();
            }
            return "direct";
        } catch (Exception e) {
            return "direct";
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private Map<String, Long> aggregateViewsByDay(List<Instant> timestamps) {
        Map<String, Long> viewsByDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            viewsByDay.put(date.toString(), 0L);
        }

        for (Instant timestamp : timestamps) {
            LocalDate date = timestamp.atZone(ZoneOffset.UTC).toLocalDate();
            String key = date.toString();
            if (viewsByDay.containsKey(key)) {
                viewsByDay.put(key, viewsByDay.get(key) + 1);
            }
        }

        return viewsByDay;
    }
}
