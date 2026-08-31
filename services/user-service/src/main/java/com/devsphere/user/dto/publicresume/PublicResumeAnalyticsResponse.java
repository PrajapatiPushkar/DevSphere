package com.devsphere.user.dto.publicresume;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicResumeAnalyticsResponse {

    private Long resumeId;
    private String publicId;
    private long totalViews;
    private long uniqueVisitors;
    private Instant lastAccessedAt;
    private Map<String, Long> viewsByDay;
    private Map<String, Long> topReferrers;

    public PublicResumeAnalyticsResponse() {
    }

    public PublicResumeAnalyticsResponse(
            Long resumeId,
            String publicId,
            long totalViews,
            long uniqueVisitors,
            Instant lastAccessedAt,
            Map<String, Long> viewsByDay,
            Map<String, Long> topReferrers) {
        this.resumeId = resumeId;
        this.publicId = publicId;
        this.totalViews = totalViews;
        this.uniqueVisitors = uniqueVisitors;
        this.lastAccessedAt = lastAccessedAt;
        this.viewsByDay = viewsByDay;
        this.topReferrers = topReferrers;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(long totalViews) {
        this.totalViews = totalViews;
    }

    public long getUniqueVisitors() {
        return uniqueVisitors;
    }

    public void setUniqueVisitors(long uniqueVisitors) {
        this.uniqueVisitors = uniqueVisitors;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Map<String, Long> getViewsByDay() {
        return viewsByDay;
    }

    public void setViewsByDay(Map<String, Long> viewsByDay) {
        this.viewsByDay = viewsByDay;
    }

    public Map<String, Long> getTopReferrers() {
        return topReferrers;
    }

    public void setTopReferrers(Map<String, Long> topReferrers) {
        this.topReferrers = topReferrers;
    }
}
