package com.devsphere.user.controller;

import com.devsphere.user.dto.CreateProjectRequest;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.ProjectResponse;
import com.devsphere.user.dto.UpdateProjectRequest;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.exception.UnauthorizedException;
import com.devsphere.user.security.UserPrincipal;
import com.devsphere.user.service.ProjectService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @Valid @RequestBody CreateProjectRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ProjectResponse response = projectService.createProject(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + response.getId())).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> listProjects(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "projectType", required = false) ProjectType projectType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        PageResponse<ProjectResponse> response = projectService.listProjects(userId, status, projectType, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long projectId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ProjectResponse response = projectService.getProject(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ProjectResponse response = projectService.updateProject(userId, projectId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<ProjectResponse> startProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long projectId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ProjectResponse response = projectService.startProject(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ProjectResponse> completeProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long projectId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ProjectResponse response = projectService.completeProject(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/hold")
    public ResponseEntity<ProjectResponse> holdProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long projectId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ProjectResponse response = projectService.holdProject(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<ProjectResponse> resumeProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long projectId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        ProjectResponse response = projectService.resumeProject(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveProject(
            @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) String authUserIdHeader,
            @PathVariable("id") Long projectId) {
        Long userId = extractAndValidateUserId(authUserIdHeader);
        projectService.archiveProject(userId, projectId);
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
