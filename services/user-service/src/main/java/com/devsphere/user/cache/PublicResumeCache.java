package com.devsphere.user.cache;

import com.devsphere.user.dto.publicresume.PublicResumeResponse;
import java.util.Optional;

public interface PublicResumeCache {

    Optional<PublicResumeResponse> get(String publicResumeId);

    void put(String publicResumeId, PublicResumeResponse response);

    void evict(String publicResumeId);
}
