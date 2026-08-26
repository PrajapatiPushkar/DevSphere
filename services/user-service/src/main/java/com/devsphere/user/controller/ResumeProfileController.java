package com.devsphere.user.controller;

import com.devsphere.user.dto.ResumeProfileRequest;
import com.devsphere.user.dto.ResumeProfileResponse;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.ResumeProfileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeProfileController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final ResumeProfileService resumeProfileService;

    public ResumeProfileController(ResumeProfileService resumeProfileService) {
        this.resumeProfileService = resumeProfileService;
    }

    @PostMapping
    public ResponseEntity<ResumeProfileResponse> createResumeProfile(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @Valid @RequestBody ResumeProfileRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeProfileResponse response = resumeProfileService.createResumeProfile(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/resumes/" + response.getId())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeProfileResponse>> listResumeProfiles(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        List<ResumeProfileResponse> responses = resumeProfileService.listResumeProfiles(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeProfileResponse> getResumeProfile(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long id) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeProfileResponse response = resumeProfileService.getResumeProfile(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeProfileResponse> updateResumeProfile(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long id,
            @Valid @RequestBody ResumeProfileRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeProfileResponse response = resumeProfileService.updateResumeProfile(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResumeProfile(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long id) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        resumeProfileService.deleteResumeProfile(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ResumeProfileResponse> archiveResumeProfile(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long id) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeProfileResponse response = resumeProfileService.archiveResumeProfile(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ResumeProfileResponse> activateResumeProfile(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long id) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeProfileResponse response = resumeProfileService.activateResumeProfile(id, userId);
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
