package com.devsphere.user.controller;

import com.devsphere.user.dto.ResumeSectionResponse;
import com.devsphere.user.dto.UpdateResumeSectionRequest;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.ResumeSectionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes/{resumeId}/sections")
public class ResumeSectionController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final ResumeSectionService resumeSectionService;

    public ResumeSectionController(ResumeSectionService resumeSectionService) {
        this.resumeSectionService = resumeSectionService;
    }

    @GetMapping
    public ResponseEntity<List<ResumeSectionResponse>> listSections(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        List<ResumeSectionResponse> responses = resumeSectionService.listSections(resumeId, userId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<ResumeSectionResponse> updateSection(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateResumeSectionRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeSectionResponse response = resumeSectionService.updateSection(resumeId, sectionId, userId, request);
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
