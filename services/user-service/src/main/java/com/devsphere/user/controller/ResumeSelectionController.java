package com.devsphere.user.controller;

import com.devsphere.user.dto.ResumeCertificationRequest;
import com.devsphere.user.dto.ResumeCertificationResponse;
import com.devsphere.user.dto.ResumeEducationRequest;
import com.devsphere.user.dto.ResumeEducationResponse;
import com.devsphere.user.dto.ResumeExperienceRequest;
import com.devsphere.user.dto.ResumeExperienceResponse;
import com.devsphere.user.dto.ResumeProjectRequest;
import com.devsphere.user.dto.ResumeProjectResponse;
import com.devsphere.user.dto.ResumeSkillRequest;
import com.devsphere.user.dto.ResumeSkillResponse;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.ResumeSelectionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes/{resumeId}")
public class ResumeSelectionController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final ResumeSelectionService resumeSelectionService;

    public ResumeSelectionController(ResumeSelectionService resumeSelectionService) {
        this.resumeSelectionService = resumeSelectionService;
    }

    // --- EXPERIENCES ---
    @PostMapping("/experiences")
    public ResponseEntity<ResumeExperienceResponse> addExperience(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeExperienceRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeExperienceResponse response = resumeSelectionService.addExperience(resumeId, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/experiences")
    public ResponseEntity<List<ResumeExperienceResponse>> listExperiences(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        List<ResumeExperienceResponse> responses = resumeSelectionService.listExperiences(resumeId, userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/experiences/{experienceId}")
    public ResponseEntity<Void> removeExperience(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long experienceId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        resumeSelectionService.removeExperience(resumeId, experienceId, userId);
        return ResponseEntity.noContent().build();
    }

    // --- EDUCATION ---
    @PostMapping("/education")
    public ResponseEntity<ResumeEducationResponse> addEducation(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeEducationRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeEducationResponse response = resumeSelectionService.addEducation(resumeId, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/education")
    public ResponseEntity<List<ResumeEducationResponse>> listEducations(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        List<ResumeEducationResponse> responses = resumeSelectionService.listEducations(resumeId, userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/education/{educationId}")
    public ResponseEntity<Void> removeEducation(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long educationId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        resumeSelectionService.removeEducation(resumeId, educationId, userId);
        return ResponseEntity.noContent().build();
    }

    // --- SKILLS ---
    @PostMapping("/skills")
    public ResponseEntity<ResumeSkillResponse> addSkill(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeSkillRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeSkillResponse response = resumeSelectionService.addSkill(resumeId, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/skills")
    public ResponseEntity<List<ResumeSkillResponse>> listSkills(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        List<ResumeSkillResponse> responses = resumeSelectionService.listSkills(resumeId, userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<Void> removeSkill(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long skillId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        resumeSelectionService.removeSkill(resumeId, skillId, userId);
        return ResponseEntity.noContent().build();
    }

    // --- CERTIFICATIONS ---
    @PostMapping("/certifications")
    public ResponseEntity<ResumeCertificationResponse> addCertification(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeCertificationRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeCertificationResponse response = resumeSelectionService.addCertification(resumeId, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/certifications")
    public ResponseEntity<List<ResumeCertificationResponse>> listCertifications(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        List<ResumeCertificationResponse> responses = resumeSelectionService.listCertifications(resumeId, userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/certifications/{certificationId}")
    public ResponseEntity<Void> removeCertification(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long certificationId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        resumeSelectionService.removeCertification(resumeId, certificationId, userId);
        return ResponseEntity.noContent().build();
    }

    // --- PROJECTS ---
    @PostMapping("/projects")
    public ResponseEntity<ResumeProjectResponse> addProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeProjectRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ResumeProjectResponse response = resumeSelectionService.addProject(resumeId, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ResumeProjectResponse>> listProjects(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        List<ResumeProjectResponse> responses = resumeSelectionService.listProjects(resumeId, userId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Void> removeProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable Long resumeId,
            @PathVariable Long projectId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        resumeSelectionService.removeProject(resumeId, projectId, userId);
        return ResponseEntity.noContent().build();
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
