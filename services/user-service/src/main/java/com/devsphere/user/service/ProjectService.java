package com.devsphere.user.service;

import com.devsphere.user.dto.CreateProjectRequest;
import com.devsphere.user.dto.PageResponse;
import com.devsphere.user.dto.ProjectResponse;
import com.devsphere.user.dto.UpdateProjectRequest;
import com.devsphere.user.entity.DeveloperProject;
import com.devsphere.user.entity.ProjectStatus;
import com.devsphere.user.entity.ProjectType;
import com.devsphere.user.exception.ResourceNotFoundException;
import com.devsphere.user.repository.DeveloperProjectRepository;
import com.devsphere.user.specification.ProjectSpecification;
import com.devsphere.user.util.PaginationUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "name", "startDate", "targetEndDate", "status", "projectType", "id"
    );

    private final DeveloperProjectRepository projectRepository;
    private final MeterRegistry meterRegistry;

    public ProjectService(DeveloperProjectRepository projectRepository) {
        this(projectRepository, new SimpleMeterRegistry());
    }

    @Autowired
    public ProjectService(DeveloperProjectRepository projectRepository, MeterRegistry meterRegistry) {
        this.projectRepository = projectRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public ProjectResponse createProject(Long userId, CreateProjectRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        validateDates(request.getStartDate(), request.getTargetEndDate());

        String trimmedName = request.getName() != null ? request.getName().trim() : null;
        if (trimmedName == null || trimmedName.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }

        log.info("Creating developer project for userId: {}, name: {}", userId, trimmedName);

        DeveloperProject project = new DeveloperProject(userId, trimmedName, request.getProjectType());
        project.setDescription(trimToNull(request.getDescription()));
        project.setRepositoryUrl(trimToNull(request.getRepositoryUrl()));
        project.setLiveUrl(trimToNull(request.getLiveUrl()));
        project.setDocumentationUrl(trimToNull(request.getDocumentationUrl()));
        project.setTechStack(request.getTechStack());
        project.setStartDate(request.getStartDate());
        project.setTargetEndDate(request.getTargetEndDate());
        project.setStatus(ProjectStatus.PLANNED);
        project.setCompletedAt(null);

        DeveloperProject saved = projectRepository.save(project);
        meterRegistry.counter("devsphere_projects_created_total", "project_type", saved.getProjectType().name()).increment();

        return new ProjectResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long userId, Long projectId) {
        DeveloperProject project = findUserProject(userId, projectId);
        return new ProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> listProjects(Long userId, ProjectStatus status, ProjectType projectType, int page, int size) {
        return listProjects(userId, status, projectType, page, size, "createdAt,desc");
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> listProjects(Long userId, ProjectStatus status, ProjectType projectType, int page, int size, String sort) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        Pageable pageable = PaginationUtils.createPageable(page, size, sort, ALLOWED_SORT_FIELDS, "createdAt", Sort.Direction.DESC);
        Specification<DeveloperProject> spec = ProjectSpecification.filterProjects(userId, status, projectType);
        Page<DeveloperProject> projectPage = projectRepository.findAll(spec, pageable);

        Page<ProjectResponse> responsePage = projectPage.map(ProjectResponse::new);
        return PageResponse.fromPage(responsePage);
    }

    @Transactional
    public ProjectResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        DeveloperProject project = findUserProject(userId, projectId);

        validateDates(request.getStartDate(), request.getTargetEndDate());

        String trimmedName = request.getName() != null ? request.getName().trim() : null;
        if (trimmedName == null || trimmedName.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }

        log.info("Updating developer project id: {} for userId: {}", projectId, userId);

        project.setName(trimmedName);
        project.setDescription(trimToNull(request.getDescription()));
        project.setProjectType(request.getProjectType());
        project.setRepositoryUrl(trimToNull(request.getRepositoryUrl()));
        project.setLiveUrl(trimToNull(request.getLiveUrl()));
        project.setDocumentationUrl(trimToNull(request.getDocumentationUrl()));
        project.setTechStack(request.getTechStack());
        project.setStartDate(request.getStartDate());
        project.setTargetEndDate(request.getTargetEndDate());

        DeveloperProject updated = projectRepository.save(project);
        return new ProjectResponse(updated);
    }

    @Transactional
    public ProjectResponse startProject(Long userId, Long projectId) {
        DeveloperProject project = findUserProject(userId, projectId);
        validateTransition(project.getStatus(), ProjectStatus.IN_PROGRESS);

        project.setStatus(ProjectStatus.IN_PROGRESS);
        DeveloperProject updated = projectRepository.save(project);
        return new ProjectResponse(updated);
    }

    @Transactional
    public ProjectResponse completeProject(Long userId, Long projectId) {
        DeveloperProject project = findUserProject(userId, projectId);
        validateTransition(project.getStatus(), ProjectStatus.COMPLETED);

        project.setStatus(ProjectStatus.COMPLETED);
        project.setCompletedAt(Instant.now());

        DeveloperProject updated = projectRepository.save(project);
        meterRegistry.counter("devsphere_projects_completed_total", "project_type", updated.getProjectType().name()).increment();
        return new ProjectResponse(updated);
    }

    @Transactional
    public ProjectResponse holdProject(Long userId, Long projectId) {
        DeveloperProject project = findUserProject(userId, projectId);
        validateTransition(project.getStatus(), ProjectStatus.ON_HOLD);

        project.setStatus(ProjectStatus.ON_HOLD);
        DeveloperProject updated = projectRepository.save(project);
        return new ProjectResponse(updated);
    }

    @Transactional
    public ProjectResponse resumeProject(Long userId, Long projectId) {
        DeveloperProject project = findUserProject(userId, projectId);

        if (project.getStatus() != ProjectStatus.ON_HOLD) {
            throw new IllegalArgumentException("Project can only be resumed from ON_HOLD status. Current status: " + project.getStatus());
        }

        project.setStatus(ProjectStatus.IN_PROGRESS);
        DeveloperProject updated = projectRepository.save(project);
        return new ProjectResponse(updated);
    }

    @Transactional
    public void archiveProject(Long userId, Long projectId) {
        DeveloperProject project = findUserProject(userId, projectId);
        validateTransition(project.getStatus(), ProjectStatus.ARCHIVED);

        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);
        meterRegistry.counter("devsphere_projects_archived_total", "project_type", project.getProjectType().name()).increment();
    }

    private DeveloperProject findUserProject(Long userId, Long projectId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID must not be null");
        }
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND", "Developer project not found with id: " + projectId));
    }

    private void validateDates(LocalDate startDate, LocalDate targetEndDate) {
        if (startDate != null && targetEndDate != null && targetEndDate.isBefore(startDate)) {
            throw new IllegalArgumentException("targetEndDate must not be before startDate");
        }
    }

    private void validateTransition(ProjectStatus currentStatus, ProjectStatus targetStatus) {
        if (currentStatus == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Cannot transition project from ARCHIVED status to " + targetStatus);
        }

        boolean valid = switch (targetStatus) {
            case IN_PROGRESS -> currentStatus == ProjectStatus.PLANNED || currentStatus == ProjectStatus.ON_HOLD;
            case COMPLETED -> currentStatus == ProjectStatus.IN_PROGRESS;
            case ON_HOLD -> currentStatus == ProjectStatus.PLANNED || currentStatus == ProjectStatus.IN_PROGRESS;
            case ARCHIVED -> currentStatus == ProjectStatus.PLANNED || currentStatus == ProjectStatus.IN_PROGRESS
                    || currentStatus == ProjectStatus.ON_HOLD || currentStatus == ProjectStatus.COMPLETED;
            case PLANNED -> false;
        };

        if (!valid) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
