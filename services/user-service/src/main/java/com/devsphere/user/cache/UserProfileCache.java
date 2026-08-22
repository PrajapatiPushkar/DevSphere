package com.devsphere.user.cache;

import com.devsphere.user.dto.UserProfileResponse;
import java.util.Optional;

public interface UserProfileCache {

    Optional<UserProfileResponse> get(Long userId);

    void put(Long userId, UserProfileResponse profile);

    void evict(Long userId);
}
