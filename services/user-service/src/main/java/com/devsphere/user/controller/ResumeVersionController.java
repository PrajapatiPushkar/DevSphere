package com.devsphere.user.controller;

import com.devsphere.user.dto.CreateResumeVersionRequest;
import com.devsphere.user.dto.ResumeVersionResponse;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.ResumeVersionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes/{resumeId}/versions")
public class ResumeVersionController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final ResumeVersionService resumeVersionService;

    public ResumeVersionController(ResumeVersionService resumeVersionService) {
        this.resumeVersionService = resumeVersionService;
    }

    @PostMapping
    public ResponseEntity<ResumeVersionResponse> createVersion(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody(required = false) CreateResumeVersionRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeVersionResponse response = resumeVersionService.createVersion(resumeId, userId, request);
        return ResponseEntity.created(URI.create("/api/v1/resumes/" + resumeId + "/versions/" + response.getId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeVersionResponse>> listVersions(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        List<ResumeVersionResponse> responses = resumeVersionService.listVersions(resumeId, userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<ResumeVersionResponse> getVersion(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long versionId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeVersionResponse response = resumeVersionService.getVersion(resumeId, versionId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{versionId}/publish")
    public ResponseEntity<ResumeVersionResponse> publishVersion(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long versionId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeVersionResponse response = resumeVersionService.publishVersion(resumeId, versionId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{versionId}/archive")
    public ResponseEntity<ResumeVersionResponse> archiveVersion(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long versionId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeVersionResponse response = resumeVersionService.archiveVersion(resumeId, versionId, userId);
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
