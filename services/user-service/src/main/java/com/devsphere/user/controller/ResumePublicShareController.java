package com.devsphere.user.controller;

import com.devsphere.user.dto.publicresume.PublicShareStatusResponse;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.PublicResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes/{resumeId}/public")
public class ResumePublicShareController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final PublicResumeService publicResumeService;

    public ResumePublicShareController(PublicResumeService publicResumeService) {
        this.publicResumeService = publicResumeService;
    }

    @PostMapping("/share")
    public ResponseEntity<PublicShareStatusResponse> enablePublicSharing(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PublicShareStatusResponse response = publicResumeService.enablePublicSharing(resumeId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<PublicShareStatusResponse> revokePublicSharing(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PublicShareStatusResponse response = publicResumeService.revokePublicSharing(resumeId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<PublicShareStatusResponse> getPublicSharingStatus(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PublicShareStatusResponse response = publicResumeService.getPublicSharingStatus(resumeId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rotate")
    public ResponseEntity<PublicShareStatusResponse> rotatePublicToken(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PublicShareStatusResponse response = publicResumeService.rotatePublicToken(resumeId, userId);
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
