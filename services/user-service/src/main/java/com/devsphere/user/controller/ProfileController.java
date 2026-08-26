package com.devsphere.user.controller;

import com.devsphere.user.dto.UpdateUserProfileRequest;
import com.devsphere.user.dto.UserProfileResponse;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final UserProfileService userProfileService;

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        UserProfileResponse response = userProfileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        UserProfileResponse response = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    private Long extractAndValidateUserId(String authUserIdHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        if (authUserIdHeader != null && !authUserIdHeader.isBlank()) {
            try {
                return Long.parseLong(authUserIdHeader.trim());
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("Invalid authenticated user identity format");
            }
        }

        throw new UnauthorizedException("Authenticated user identity is required");
    }
}
