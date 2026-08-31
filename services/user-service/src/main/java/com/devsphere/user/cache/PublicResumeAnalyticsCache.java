package com.devsphere.user.cache;

import com.devsphere.user.dto.publicresume.PublicResumeAnalyticsResponse;
import java.util.Optional;

public interface PublicResumeAnalyticsCache {

    Optional<PublicResumeAnalyticsResponse> get(Long resumeId);

    void put(Long resumeId, PublicResumeAnalyticsResponse response);

    void evict(Long resumeId);
}
